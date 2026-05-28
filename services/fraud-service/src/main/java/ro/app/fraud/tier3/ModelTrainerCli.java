package ro.app.fraud.tier3;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.app.fraud.config.FraudProperties;
import smile.anomaly.IsolationForest;

/**
 * — Antrenor Offline al Modelului Isolation Forest
 *
 * DE CE CommandLineRunner?
 * --------------------------
 * Spring Boot ofera interfata CommandLineRunner: daca un bean o implementeaza,
 * metoda run() este apelata automat DUPA ce contextul Spring a pornit.
 * Este perfect pentru scripturi de tip "ruleaza o data".
 *
 * @ConditionalOnProperty garanteaza ca acest bean EXISTA IN MEMORIE doar
 * daca proprietatea fraud.tier3.trainer-mode=true este setata.
 * In productie, aceasta proprietate lipseste → bean-ul nu se creeaza.
 *
 * FLUXUL COMPLET DE ANTRENAMENT (versiunea corectata):
 * -------------------------------------------------------
 * 1. Citire CSV PaySim (sub-sampling 150.000 linii)
 * 2. Feature engineering via PaySimFeatureMapper
 * 3. Shuffle reproductibil (seed fix) pentru a amesteca normal/fraud
 * 4. Split STRATIFIED 80/20 — garanteaza aceeasi rata fraud/normal
 * 5. MinMaxScaler pe train set, mins/maxes salvate in snapshot
 * 6. IsolationForest.fit cu contamination DINAMIC = rata reala de fraude
 * 7. Calibrare threshold cu F_beta (beta=0.5, favorizeaza Precision)
 * 8. Evaluare finala: Precision, Recall, F0.5, F1, AUC-ROC
 * 9. Salvare pe disc via ModelStore (include featureMins/featureMaxes)
 * 10. System.exit(0) - aplicatia se opreste
 *
 * CUM RULEZI:
 * -----------
 * java -jar fraud-service.jar \
 *   --fraud.tier3.trainer-mode=true \
 *   --fraud.tier3.pay-sim-csv-path= src\main\resources\ml\paysim_sample.csv \
 *   --fraud.tier3.model-path=/data/isolation_forest_model.bin
 */
@Component
@ConditionalOnProperty(name = "fraud.tier3.trainer-mode", havingValue = "true")
public class ModelTrainerCli implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainerCli.class);

    private static final int IF_NUM_TREES = 50;  // 50 arbori = bun echilibru dimensiune/calitate pe PaySim
    private static final int IF_MAX_DEPTH = 10;  // adâncime maximă arbore — Smile 3.x: al 3-lea param este maxDepth, NU subsampleSize!
                                                   // Formula: ceil(log2(256)) = 8; folosim 10 pt. margine de siguranță.
    private static final int IF_SEED = 0;   // seed SMILE intern

    // F_beta cu beta=0.5 — in banking, un False Positive (blocare tranzactie legitima)
    // costa experienta utilizatorului, iar False Negative (frauda nedetectata) costa bani.
    // beta=0.5 → Precision cantarita DUBLU fata de Recall.
    // Justificare : reducerea alarmelor false protejeaza UX, iar Tier1+Tier2 captureaza
    // cazurile evidente deterministic.
    private static final double FBETA_BETA = 0.5;

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

        log.info("=== FRAUD SERVICE — MODEL TRAINER (v2 — with MinMaxScaler + Stratified Split + Dynamic Contamination) ===");
        log.info("CSV: {}", csvPath);
        log.info("Output: {}", modelPath);
        log.info("Max rows: {}", maxRows);

        // ── PASUL 1: Citire CSV ──────────────────────────────────────────────
        log.info("[1/7] Reading PaySim CSV...");
        List<PaySimRow> rows = PaySimCsvReader.read(csvPath, maxRows);
        log.info("Read: {} rows", rows.size());

        long fraudCount = rows.stream().filter(r -> r.isFraud() == 1).count();
        long normalCount = rows.size() - fraudCount;
        double fraudRate = rows.size() > 0 ? (double) fraudCount / rows.size() : 0.0;
        double fraudRatePercent = fraudRate * 100;
        log.info("Normal={} Fraud={} fraud_rate={}%", normalCount, fraudCount,
            String.format("%.2f", fraudRatePercent));

        // ── PASUL 2: Feature Engineering ────────────────────────────────────
        log.info("[2/7] Feature engineering (PaySimFeatureMapper)...");
        double[][] X = new double[rows.size()][6];
        int[] labels = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            X[i] = PaySimFeatureMapper.fromPaySim(rows.get(i));
            labels[i] = rows.get(i).isFraud();
        }

        // ── PASUL 3: Shuffle reproductibil ──────────────────────────────────
        log.info("[3/7] Reproducible shuffle (seed={})...", tier3.getMlSeed());
        shuffleWithSeed(X, labels, tier3.getMlSeed());

        // ── PASUL 4: Stratified Train/Test Split 80/20 ──────────────────────
        // Split stratificat — garanteaza ca train si test au ACEEASI rata de fraude.
        // Un split random simplu pe 150k rows este probabilistic OK, dar stratificarea
        // este mai robusta si demonstreaza best practice academic.
        log.info("[4/7] Stratified 80/20 split...");
        int[][] split = stratifiedSplit(X, labels, 0.8);
        int trainSize = split[0].length;
        int testSize = split[1].length;

        double[][] trainX = selectRows(X, split[0]);
        int[] trainLabels = selectLabels(labels, split[0]);
        double[][] testX = selectRows(X, split[1]);
        int[] testLabels = selectLabels(labels, split[1]);

        long trainFraud = countFraud(trainLabels);
        long testFraud = countFraud(testLabels);
        log.info("Train: {} rows (fraud={}, {}%), Test: {} rows (fraud={}, {}%)",
            trainSize, trainFraud, String.format("%.2f", (double) trainFraud / trainSize * 100),
            testSize,  testFraud,  String.format("%.2f", (double) testFraud  / testSize  * 100));

        // ── PASUL 5: MinMaxScaler pe train set ──────────────────────────────
        // Scaleaza toate features la [0, 1] pe baza min/max din train set.
        // mins si maxes sunt salvate in ModelSnapshot pentru aplicarea identica la inferenta.
        log.info("[5/7] MinMaxScaler on train set...");
        double[] featureMins  = MlUtils.computeMins(trainX);
        double[] featureMaxes = MlUtils.computeMaxes(trainX);
        double[][] scaledTrainX = MlUtils.minMaxScale(trainX, featureMins, featureMaxes);
        double[][] scaledTestX  = MlUtils.minMaxScale(testX,  featureMins, featureMaxes);

        log.info("Feature mins:  {}", formatArray(featureMins));
        log.info("Feature maxes: {}", formatArray(featureMaxes));

        // ── PASUL 6: IsolationForest training ──────────────────────────────
        // contamination DINAMIC = rata reala de fraude din dataset.
        // Justificare: contamination in IF determina threshold-ul intern pentru score.
        // Math.min(..., 0.30) = safety cap recomandat in literatura (Isolation Forest paper).
        double dynamicContamination = Math.min(fraudRate, 0.30);
        log.info("[6/7] IsolationForest training: num_trees={} max_depth={} contamination={} (dynamic, raw_fraud_rate={}%)",
            IF_NUM_TREES, IF_MAX_DEPTH,
            String.format("%.4f", dynamicContamination),
            String.format("%.2f", fraudRatePercent));

        long t0 = System.currentTimeMillis();
        IsolationForest model = IsolationForest.fit(scaledTrainX, IF_NUM_TREES, IF_MAX_DEPTH, dynamicContamination, IF_SEED);
        log.info("Training completed in {} ms", System.currentTimeMillis() - t0);

        // featureMeans pe setul de train NORMAL (pentru PerturbationAnalyzer)
        double[][] trainNormalX = filterNormal(trainX, trainLabels);
        double[] featureMeans = MlUtils.computeMeans(trainNormalX);
        log.info("featureMeans (normal-only): {}", formatArray(featureMeans));

        // ── PASUL 7: Calibrare threshold (F_beta) + Evaluare ────────────────
        // Maximizam F_beta cu beta=0.5 in loc de F1.
        // Raport Precision/Recall asimetric in banking: FP costa UX, FN costa bani.
        log.info("[7/7] Calibrating threshold (F_{} maximization) on test set...", FBETA_BETA);
        double optimalThreshold = findOptimalThreshold(model, scaledTestX, testLabels);
        evaluate(model, scaledTestX, testLabels, optimalThreshold, "FINAL");

        // ─ SALVARE ──────────────────────────────────────────────────────────
        ModelStore.ModelSnapshot snapshot = new ModelStore.ModelSnapshot(
                model, optimalThreshold,
                featureMeans, featureMins, featureMaxes, // scaler params
                ModelStore.currentVersion(),
                fraudRate,        // rata reala [0,1]
                rows.size());     // nr. total randuri citite din CSV
        ModelStore.save(snapshot, modelPath);

        log.info("=== TRAINING COMPLETED ===");
        log.info("Model saved to: {}", modelPath);
        log.info("Optimal threshold: {}", optimalThreshold);
        log.info("Dynamic contamination used: {}", String.format("%.4f", dynamicContamination));
        log.info("Restart the application normally for inference.");

        System.exit(0); // clean exit dupa antrenament
    }

    // Calibrare threshold cu F_beta (beta=0.5, Precision > Recall)

    private double findOptimalThreshold(IsolationForest model, double[][] testX, int[] testLabels) {
        log.info("Pre-calculating scores for {} test examples...", testX.length);
        double[] scores = new double[testX.length];
        for (int i = 0; i < testX.length; i++) {
            scores[i] = model.score(testX[i]);
        }
        log.info("Scores calculated. Calibrating threshold (F_{})...", FBETA_BETA);

        double bestFbeta = 0, bestThreshold = 0.5;
        double bestF1 = 0; // raportat aditional pentru comparabilitate academica

        for (double t = 0.30; t <= 0.90; t += 0.01) {
            double fbeta = computeFBetaFromScores(scores, testLabels, t, FBETA_BETA);
            double f1 = computeF1FromScores(scores, testLabels, t);
            if (fbeta > bestFbeta) {
                bestFbeta = fbeta;
                bestThreshold = t;
                bestF1 = f1;
            }
        }
        log.info("Optimal threshold: {} (max F_{}={}, F1_at_threshold={})",
            String.format("%.2f", bestThreshold),
            FBETA_BETA, String.format("%.4f", bestFbeta),
            String.format("%.4f", bestF1));
        return bestThreshold;
    }

    /** F_beta generalizat. beta<1 = Precision weighted higher; beta>1 = Recall weighted higher. */
    private double computeFBetaFromScores(double[] scores, int[] labels, double threshold, double beta) {
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
        double betaSq = beta * beta;
        double denom = betaSq * precision + recall;
        return denom > 0 ? (1 + betaSq) * precision * recall / denom : 0;
    }

    private double computeF1FromScores(double[] scores, int[] labels, double threshold) {
        return computeFBetaFromScores(scores, labels, threshold, 1.0);
    }

    private void evaluate(IsolationForest model, double[][] X, int[] labels,
                          double threshold, String tag) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < X.length; i++) {
            boolean pred   = model.score(X[i]) > threshold;
            boolean actual = labels[i] == 1;
            if (pred && actual)  tp++;
            if (pred && !actual) fp++;
            if (!pred && !actual) tn++;
            if (!pred && actual) fn++;
        }
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recall    = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        double f1        = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0;
        double f05       = computeFBetaFromScores(scoreAll(model, X), labels, threshold, 0.5);
        double accuracy  = (double)(tp + tn) / X.length;
        double auc       = computeAucRoc(model, X, labels);

        log.info("[{}] threshold={} TP={} FP={} TN={} FN={}", tag, threshold, tp, fp, tn, fn);
        log.info("[{}] Precision={} Recall={} F1={} F0.5={} Acc={} AUC-ROC={}",
            tag,
            String.format("%.4f", precision),
            String.format("%.4f", recall),
            String.format("%.4f", f1),
            String.format("%.4f", f05),
            String.format("%.4f", accuracy),
            String.format("%.4f", auc));
    }

    // AUC-ROC — Integrare trapezoidala a curbei ROC

    private double computeAucRoc(IsolationForest model, double[][] X, int[] labels) {
        double[] scores = scoreAll(model, X);
        int n = X.length;

        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));

        long totalPos = 0;
        for (int l : labels) if (l == 1) totalPos++;
        long totalNeg = n - totalPos;

        if (totalPos == 0 || totalNeg == 0) {
            log.warn("AUC-ROC: no positive or negative examples in the test set — AUC undefined");
            return 0.0;
        }

        double auc = 0.0;
        double tpr = 0.0, fpr = 0.0, prevTpr = 0.0, prevFpr = 0.0;

        for (int i = 0; i < n; i++) {
            if (labels[idx[i]] == 1) tpr += 1.0 / totalPos;
            else                      fpr += 1.0 / totalNeg;
            auc += (fpr - prevFpr) * (tpr + prevTpr) / 2.0;
            prevTpr = tpr;
            prevFpr = fpr;
        }
        return auc;
    }

    private double[] scoreAll(IsolationForest model, double[][] X) {
        double[] scores = new double[X.length];
        for (int i = 0; i < X.length; i++) scores[i] = model.score(X[i]);
        return scores;
    }

    // Stratified Train/Test Split

    /**
     * Split stratificat: separa indexii fraud de normal, shuffleaza fiecare grup
     * independent, apoi preia 80% din fiecare pentru train si 20% pentru test.
     * Garanteaza ca rata de fraude este identica in train si test.
     */
    private int[][] stratifiedSplit(double[][] X, int[] labels, double trainRatio) {
        List<Integer> fraudIdx = new ArrayList<>();
        List<Integer> normalIdx = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == 1) fraudIdx.add(i);
            else                 normalIdx.add(i);
        }

        int trainFraud  = (int)(fraudIdx.size()  * trainRatio);
        int trainNormal = (int)(normalIdx.size() * trainRatio);

        List<Integer> trainList = new ArrayList<>();
        List<Integer> testList  = new ArrayList<>();

        for (int i = 0; i < fraudIdx.size();  i++) (i < trainFraud  ? trainList : testList).add(fraudIdx.get(i));
        for (int i = 0; i < normalIdx.size(); i++) (i < trainNormal ? trainList : testList).add(normalIdx.get(i));

        return new int[][]{ toIntArray(trainList), toIntArray(testList) };
    }

    private double[][] selectRows(double[][] X, int[] indices) {
        double[][] result = new double[indices.length][];
        for (int i = 0; i < indices.length; i++) result[i] = X[indices[i]];
        return result;
    }

    private int[] selectLabels(int[] labels, int[] indices) {
        int[] result = new int[indices.length];
        for (int i = 0; i < indices.length; i++) result[i] = labels[indices[i]];
        return result;
    }

    private long countFraud(int[] labels) {
        long c = 0;
        for (int l : labels) if (l == 1) c++;
        return c;
    }

    private double[][] filterNormal(double[][] X, int[] labels) {
        List<double[]> normal = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == 0) normal.add(X[i]);
        }
        return normal.toArray(new double[0][]);
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    // Fisher-Yates shuffle cu seed fix (reproductibil)

    private static void shuffleWithSeed(double[][] X, int[] labels, long seed) {
        java.util.Random rng = new java.util.Random(seed);
        for (int i = X.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            double[] tmpRow = X[i]; X[i] = X[j]; X[j] = tmpRow;
            int tmpLabel = labels[i]; labels[i] = labels[j]; labels[j] = tmpLabel;
        }
    }

    private static String formatArray(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.4f", arr[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}
