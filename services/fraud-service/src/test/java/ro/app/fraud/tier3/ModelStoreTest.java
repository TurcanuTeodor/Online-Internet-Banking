package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import smile.anomaly.IsolationForest;

/**
 * Teste unitare pentru ModelStore — serializare si deserializare snapshot ML.
 */
class ModelStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_roundTrip_preservesAllData() throws Exception {
        // Arrange
        Path modelPath = tempDir.resolve("test_model.bin");
        
        // Creăm un model dummy IF (necesită antrenare sumară pentru a nu fi null/invalid intern)
        double[][] dummyData = {{0.1, 0.2}, {0.3, 0.4}};
        IsolationForest dummyModel = IsolationForest.fit(dummyData, 10, 5, 0.1, 0);

        double[] means = {0.15, 0.30, 0.0, 0.0, 0.0, 0.0};
        double[] mins  = {0.10, 0.20, 0.0, 0.0, 0.0, 0.0};
        double[] maxes = {0.30, 0.40, 0.0, 0.0, 0.0, 0.0};
        
        ModelStore.ModelSnapshot original = new ModelStore.ModelSnapshot(
                dummyModel, 0.65, means, mins, maxes,
                ModelStore.currentVersion(), 0.12, 1000
        );

        // Act - Salvare
        ModelStore.save(original, modelPath.toString());
        assertTrue(Files.exists(modelPath), "Fișierul trebuie să fi fost creat");

        // Act - Încărcare
        ModelStore.ModelSnapshot loaded = ModelStore.load(modelPath.toString());

        // Assert
        assertNotNull(loaded.model, "Modelul IF trebuie sa fie deserializat");
        assertEquals(0.65, loaded.threshold, 1e-9);
        assertEquals(ModelStore.currentVersion(), loaded.version);
        assertEquals(0.12, loaded.trainedFraudRate, 1e-9);
        assertEquals(1000, loaded.trainedOnRows);
        
        assertArrayEquals(means, loaded.getFeatureMeans(), 1e-9);
        assertArrayEquals(mins, loaded.getFeatureMins(), 1e-9);
        assertArrayEquals(maxes, loaded.getFeatureMaxes(), 1e-9);
    }

    @Test
    void load_nonExistentFile_throwsFileNotFoundException() {
        assertThrows(java.io.FileNotFoundException.class, () -> {
            ModelStore.load(tempDir.resolve("does_not_exist.bin").toString());
        });
    }

    @Test
    void exists_returnsTrueForExistingFile() throws IOException {
        Path modelPath = tempDir.resolve("dummy.bin");
        Files.createFile(modelPath);
        
        assertTrue(ModelStore.exists(modelPath.toString()));
    }

    @Test
    void exists_returnsFalseForMissingFile() {
        assertFalse(ModelStore.exists(tempDir.resolve("missing.bin").toString()));
    }
}
