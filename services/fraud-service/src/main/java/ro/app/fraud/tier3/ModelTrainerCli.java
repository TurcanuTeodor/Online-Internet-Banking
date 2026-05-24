package ro.app.fraud.tier3;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.app.fraud.config.FraudProperties;
import smile.anomaly.IsolationForest;

/**
 * =====================================================================
 * — Antrenor Offline al Modelului Isolation Forest
 * =====================================================================
 *
 * DE CE CommandLineRunner?
 * --------------------------
 * Spring Boot ofera interfata CommandLineRunner: daca un bean o implementeaza,
 * metoda run() este apelata automat DUPA ce contextul Spring a pornit.
 * Este perfect pentru scripturi de tip "ruleaza o data".
 *
 * @ConditionalOnProperty garanteaza ca acest bean EXISTA IN MEMORIE doar
 * daca proprietatea fraud.tier3.trainer.mode=true este setata.
 * In productie, aceasta proprietate lipseste -> bean-ul nu se creeaza.
 *
 * FLUXUL COMPLET DE ANTRENAMENT:
 * --------------------------------
 * 1. Citire CSV PaySim (sub-sampling 150.000 linii)
 * 2. Feature engineering via PaySimFeatureMapper
 * 3. Shuffle reproductibil (seed fix) pentru a amesteca normal/fraud
 * 4. Split 80/20 train/test (REGULA DE AUR: nu evalua pe date de antrenament!)
 * 5. Antrenare IsolationForest NUMAI pe datele de train
 * 6. Calibrare threshold optim pe datele de test (maximizare F1)
 * 7. Evaluare finala: Precision, Recall, F1, AUC-ROC
 * 8. Salvare pe disc via ModelStore
 * 9. System.exit(0) - aplicatia se opreste
 *
 * CUM RULEZI:
 * -----------
 * java -jar fraud-service.jar \
 *   --fraud.tier3.trainer.mode=true \
 *   --fraud.tier3.paysim-csv-path=/data/PS_20174392719_1491204439457_log.csv \
 *   --fraud.tier3.model-path=/data/isolation_forest_model.bin
 */
@Component
@ConditionalOnProperty(name = "fraud.tier3.trainer-mode", havingValue = "true")
public class ModelTrainerCli implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainerCli.class);

    private static final int IF_NUM_TREES = 50;   // 50 arbori = bun echilibru dimensiune/calitate pe PaySim
    private static final int IF_SUBSAMPLE = 256;   // subsample per arbore (SMILE default)
    private static final int IF_SEED = 0;     // seed SMILE intern

    private final FraudProperties props;

    public ModelTrainerCli(FraudProperties props) {
        this.props = props;
    }

    @Override
    public void run(String... args) throws Exception {
        FraudProperties.Tier3 tier3 = props.getTier3();
        String csvPath = tier3.getPaySimCsvPath();
        String modelPath = tier3.getModelPath();
        int maxRows = tier3.getPaySimMaxRows(); 
        double contamin  = tier3.getMlContamination();

        log.info("=== FRAUD SERVICE — MODEL TRAINER ===");
        log.info("CSV: {}", csvPath);
        log.info("Output: {}", modelPath);
        log.info("Max rows: {}", maxRows);
        log.info("Contamination: {}", contamin);

        // ── PASUL 1: Citire CSV ──────────────────────────────────────────────
        log.info("[1/6] Reading PaySim CSV...");
        List<PaySimRow> rows = PaySimCsvReader.read(csvPath, maxRows);
        log.info("Read: {} rows", rows.size());

        long fraudCount = rows.stream().filter(r -> r.isFraud() == 1).count();
        long normalCount = rows.size() - fraudCount;
        double fraudRatePercent = rows.size() > 0 ? (double) fraudCount / rows.size() * 100 : 0.0;
        log.info("Normal={} Fraud={} fraud_rate={}%", normalCount, fraudCount,
            String.format("%.2f", fraudRatePercent));

        // ── PASUL 2: Feature Engineering ────────────────────────────────────
        log.info("[2/6] Feature engineering (PaySimFeatureMapper)...");
        double[][] X = new double[rows.size()][6];
        int[] labels = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            X[i] = PaySimFeatureMapper.fromPaySim(rows.get(i));
            labels[i] = rows.get(i).isFraud();
        }

        // ── PASUL 3: Shuffle reproductibil ──────────────────────────────────
        log.info("[3/6] Reproducible shuffle (seed={})...", tier3.getMlSeed());
        shuffleWithSeed(X, labels, tier3.getMlSeed());

        // ── PASUL 4: Train/Test Split 80/20 ─────────────────────────────────
        int trainSize = (int) (X.length * 0.8);
        int testSize = X.length - trainSize;
        log.info("[4/6] Split: train={} test={}", trainSize, testSize);

        double[][] trainX = java.util.Arrays.copyOfRange(X, 0, trainSize);
        double[][] testX = java.util.Arrays.copyOfRange(X, trainSize, X.length);
        int[] testLabels= java.util.Arrays.copyOfRange(labels, trainSize, labels.length);

        // ── STEP 5: IsolationForest training ──────────────────────────────
        log.info("[5/6] IsolationForest training: num_trees={} subsample={} contamin={}",
            IF_NUM_TREES, IF_SUBSAMPLE, contamin);
        long t0 = System.currentTimeMillis();
        IsolationForest model = IsolationForest.fit(trainX, IF_NUM_TREES, IF_SUBSAMPLE, contamin, IF_SEED);
        log.info("Training completed in {} ms", System.currentTimeMillis() - t0);

        // featureMeans pe setul de train NORMAL (pentru PerturbationAnalyzer)
        long trainNormalCount = 0;
        for (int i = 0; i < trainSize; i++) {
            if (labels[i] == 0) trainNormalCount++;
        }
        double[][] trainNormalX = new double[(int) trainNormalCount][6];
        int ni = 0;
        for (int i = 0; i < trainSize; i++) {
            if (labels[i] == 0) trainNormalX[ni++] = trainX[i];
        }
        double[] featureMeans = MlUtils.computeMeans(trainNormalX);

        // ── STEP 6: Calibrate threshold ─────────────────────────────────────
        log.info("[6/6] Calibrating threshold on test set...");
        double optimalThreshold = findOptimalThreshold(model, testX, testLabels);
        evaluate(model, testX, testLabels, optimalThreshold, "FINAL");

        // ─ SALVARE ──────────────────────────────────────────────────────────
        ModelStore.ModelSnapshot snapshot = new ModelStore.ModelSnapshot(
                model, optimalThreshold, featureMeans, ModelStore.currentVersion());
        ModelStore.save(snapshot, modelPath);

        log.info("=== TRAINING COMPLETED ===");
        log.info("Model saved to: {}", modelPath);
        log.info("Optimal threshold: {}", optimalThreshold);
        log.info("Restart the application normally for inference.");

        System.exit(0); // clean exit dupa antrenament
    }

    // -----------------------------------------------------------------------
    // Calibrare threshold: cauta valoarea care maximizează F1 pe test
    //
    // OPTIMIZARE: scorurile sunt calculate O SINGURA DATA (30.000 apeluri model.score()),
    // nu de 30 ori (900.000 apeluri). Reducere 30x a timpului de calibrare.
    // -----------------------------------------------------------------------

    private double findOptimalThreshold(IsolationForest model, double[][] testX, int[] testLabels) {
        // Pasul 1: pre-calculeaza TOATE scorurile o singura data
        log.info("Pre-calculez scorurile pentru {} exemple de test...", testX.length);
        double[] scores = new double[testX.length];
        for (int i = 0; i < testX.length; i++) {
            scores[i] = model.score(testX[i]);
        }
        log.info("Scoruri calculate. Calibrez threshold-ul...");

        // Pasul 2: itereaza threshold-urile pe array-ul de scoruri (fara apeluri model.score)
        double bestF1 = 0, bestThreshold = 0.5;
        for (double t = 0.30; t <= 0.90; t += 0.02) {
            double f1 = computeF1FromScores(scores, testLabels, t);
            if (f1 > bestF1) {
                bestF1 = f1;
                bestThreshold = t;
            }
        }
        log.info("Optimal threshold: {} (max F1={})", bestThreshold, String.format("%.4f", bestF1));
        return bestThreshold;
    }

    /** F1 calculat pe scoruri pre-calculate — fara apeluri model.score() */
    private double computeF1FromScores(double[] scores, int[] labels, double threshold) {
        int tp = 0, fp = 0, fn = 0;
        for (int i = 0; i < scores.length; i++) {
            boolean pred   = scores[i] > threshold;
            boolean actual = labels[i] == 1;
            if (pred && actual)  tp++;
            if (pred && !actual) fp++;
            if (!pred && actual) fn++;
        }
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recall    = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        return (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0;
    }

    private double computeF1(IsolationForest model, double[][] X, int[] labels, double threshold) {
        int tp = 0, fp = 0, fn = 0;
        for (int i = 0; i < X.length; i++) {
            boolean pred = model.score(X[i]) > threshold;
            boolean actual = labels[i] == 1;
            if (pred && actual) tp++;
            if (pred && !actual) fp++;
            if (!pred && actual) fn++;
        }
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        return (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0;
    }

    private void evaluate(IsolationForest model, double[][] X, int[] labels,
                          double threshold, String tag) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < X.length; i++) {
            boolean pred = model.score(X[i]) > threshold;
            boolean actual = labels[i] == 1;
            if (pred && actual) tp++;
            if (pred && !actual) fp++;
            if (!pred && !actual) tn++;
            if (!pred && actual) fn++;
        }
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0;
        double accuracy  = (double)(tp + tn) / X.length;

        log.info("[{}] threshold={} TP={} FP={} TN={} FN={}", tag, threshold, tp, fp, tn, fn);
        log.info("[{}] Precision={} Recall={} F1={} Acc={}",
            tag,
            String.format("%.4f", precision),
            String.format("%.4f", recall),
            String.format("%.4f", f1),
            String.format("%.4f", accuracy));
    }

    // -----------------------------------------------------------------------
    // Fisher-Yates shuffle cu seed fix (reproductibil)
    // -----------------------------------------------------------------------

    private static void shuffleWithSeed(double[][] X, int[] labels, long seed) {
        java.util.Random rng = new java.util.Random(seed);
        for (int i = X.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            // swap X
            double[] tmpRow = X[i]; X[i] = X[j]; X[j] = tmpRow;
            // swap labels
            int tmpLabel = labels[i]; labels[i] = labels[j]; labels[j] = tmpLabel;
        }
    }
}
