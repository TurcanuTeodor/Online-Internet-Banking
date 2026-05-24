package ro.app.fraud.tier3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import smile.anomaly.IsolationForest;

/**
 * =====================================================================
 * — Serializare/Deserializare Model pe Disc
 * =====================================================================
 *
 * DE CE serializare? 
 * -------------------------------------------------------
 * Antrenarea unui Isolation Forest pe 150.000 de randuri dureaza ~5-15 secunde.
 * Daca am antrena la fiecare pornire a aplicației:
 *   - Cold Start in productie → tranzactiile din primele secunde NU sunt evaluate
 *   - Fiecare restart Docker → asteptare inutila
 *   - Threshold-ul calibrat s-ar recalcula → instabilitate între deployments
 *
 * Solutia: antrenare O SINGURA DATA, save starea completa pe disc.
 * La fiecare pornire ulterioara → incarcam din disc in ~100ms.
 *
 * CE SALVAM (ModelSnapshot = container serializabil):
 * - modelul IsolationForest (arborii deja construiti)
 * - threshold-ul optim calibrat pe setul de test
 * - featureMeans (pentru PerturbationAnalyzer)
 * - versiunea modelului (pentru audit trail)
 *
 * MECANISM: Java ObjectOutputStream → .bin
 * IsolationForest din SMILE implementeaza Serializable, deci merge direct.
 */
public final class ModelStore {

    private static final Logger log = LoggerFactory.getLogger(ModelStore.class);
    private static final String CURRENT_MODEL_VERSION = "paysim-v1.0";

    private ModelStore() {}

    // -----------------------------------------------------------------------
    // Snapshot intern (ce se salvează pe disc)
    // -----------------------------------------------------------------------

    public static final int EXPECTED_FEATURE_COUNT = 6;

    public static class ModelSnapshot implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public final IsolationForest model;
        public final double threshold;
        private final double[] featureMeans;  
        public final String version;
        public final long trainedAtEpoch; 

        public ModelSnapshot(IsolationForest model, double threshold,
                             double[] featureMeans, String version) {
            this.model = model;
            this.threshold = threshold;
            this.featureMeans = featureMeans.clone(); 
            this.version = version;
            this.trainedAtEpoch = System.currentTimeMillis();
        }

        public double[] getFeatureMeans() {
            return java.util.Arrays.copyOf(featureMeans, featureMeans.length);
        }
    }

    // -----------------------------------------------------------------------
    // SALVARE
    // -----------------------------------------------------------------------

    public static void save(ModelSnapshot snapshot, String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent()); 

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(path.toFile())))) {
            oos.writeObject(snapshot);
        }

        long sizeKb = Files.size(path) / 1024;
        log.info("Model saved: path={} size={}KB version={}", outputPath, sizeKb, snapshot.version);
    }

    // -----------------------------------------------------------------------
    // INCARCARE
    // -----------------------------------------------------------------------

    // Deserializeaza snapshot-ul 
    public static ModelSnapshot load(String modelPath) throws IOException, ClassNotFoundException {
        InputStream is;
        if (modelPath.startsWith("classpath:")) {
            String cpPath = modelPath.substring(10);
            ClassPathResource resource = new ClassPathResource(cpPath);
            if (!resource.exists()) {
                throw new FileNotFoundException("Model nu a fost gasit in classpath: " + cpPath);
            }
            is = resource.getInputStream();
        } else {
            Path path = Paths.get(modelPath);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Model not found: " + modelPath
                        + "\n  → Run first: java -jar fraud-service.jar --fraud.tier3.trainer-mode=true");
            }
            is = new FileInputStream(path.toFile());
        }

        ModelSnapshot snapshot;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is))) {
            snapshot = (ModelSnapshot) ois.readObject();
        }

        // Validare versiune 
        if (!CURRENT_MODEL_VERSION.equals(snapshot.version)) {
            log.warn("Version mismatch! Disc={} Expected={}. Retrain the model.",
                    snapshot.version, CURRENT_MODEL_VERSION);
        }

        double[] means = snapshot.getFeatureMeans();
        if (means == null || means.length != EXPECTED_FEATURE_COUNT) {
            throw new IllegalStateException(String.format(
                "Model corupt or incompatible: featureMeans.length=%s, expected %d. " +
                "Retrain the model with --fraud.tier3.trainer.mode=true.",
                means == null ? "null" : means.length, EXPECTED_FEATURE_COUNT));
        }

        long trainedAgo = (System.currentTimeMillis() - snapshot.trainedAtEpoch) / (1000L * 60 * 60 * 24);
        log.info("Model loaded: version={} threshold={} features={} trained {} days ago",
                snapshot.version, snapshot.threshold, means.length, trainedAgo);

        return snapshot;
    }

    // -----------------------------------------------------------------------
    // UTILS
    // -----------------------------------------------------------------------

    //verifica daca modelul exista
    public static boolean exists(String modelPath) {
        if (modelPath.startsWith("classpath:")) {
            return new ClassPathResource(modelPath.substring(10)).exists();
        }
        return Files.exists(Paths.get(modelPath));
    }

    public static String currentVersion() {
        return CURRENT_MODEL_VERSION;
    }
}
