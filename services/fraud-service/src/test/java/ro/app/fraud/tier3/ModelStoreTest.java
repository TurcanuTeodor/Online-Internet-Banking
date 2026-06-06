package ro.app.fraud.tier3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import smile.anomaly.IsolationForest;

/**
 * Teste unitare pentru ModelStore (JUnit 4).
 *
 * Testeaza serializarea si deserializarea snapshot-ului ML.
 * Folosim @Rule TemporaryFolder (JUnit 4).
 *
 * Pattern testat: Repository (Structural) — salvare si incarcare model din
 * fisier.
 */
@RunWith(JUnit4.class)
public class ModelStoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void saveAndLoad_roundTrip_preservesAllData() throws Exception {
        // Arrange
        Path modelPath = tempFolder.newFile("test_model.bin").toPath();

        // Cream un model Isolation Forest dummy
        double[][] dummyData = { { 0.1, 0.2 }, { 0.3, 0.4 } };
        IsolationForest dummyModel = IsolationForest.fit(dummyData, 10, 5, 0.1, 0);

        double[] means = { 0.15, 0.30, 0.0, 0.0, 0.0, 0.0 };
        double[] mins = { 0.10, 0.20, 0.0, 0.0, 0.0, 0.0 };
        double[] maxes = { 0.30, 0.40, 0.0, 0.0, 0.0, 0.0 };

        ModelStore.ModelSnapshot original = new ModelStore.ModelSnapshot(
                dummyModel, 0.65, means, mins, maxes,
                ModelStore.currentVersion(), 0.12, 1000);

        // Act — salvare
        ModelStore.save(original, modelPath.toString());
        assertTrue("File must be created", Files.exists(modelPath));

        // Act — incarcare
        ModelStore.ModelSnapshot loaded = ModelStore.load(modelPath.toString());

        // Assert
        assertNotNull("IF model must be deserialized", loaded.model);
        assertEquals(0.65, loaded.threshold, 1e-9);
        assertEquals(ModelStore.currentVersion(), loaded.version);
        assertEquals(0.12, loaded.trainedFraudRate, 1e-9);
        assertEquals(1000, loaded.trainedOnRows);

        assertArrayEquals(means, loaded.getFeatureMeans(), 1e-9);
        assertArrayEquals(mins, loaded.getFeatureMins(), 1e-9);
        assertArrayEquals(maxes, loaded.getFeatureMaxes(), 1e-9);
    }

    @Test(expected = java.io.FileNotFoundException.class)
    public void load_nonExistentFile_throwsFileNotFoundException() throws Exception {
        // Arrange
        String nonExistentPath = tempFolder.getRoot().getAbsolutePath() + "/does_not_exist.bin";

        // Act
        ModelStore.load(nonExistentPath);
    }

    @Test
    public void exists_existingFile_returnsTrue() throws IOException {
        // Arrange
        Path modelPath = tempFolder.newFile("dummy.bin").toPath();

        // Assert
        assertTrue(ModelStore.exists(modelPath.toString()));
    }

    @Test
    public void exists_missingFile_returnsFalse() {
        // Arrange
        String missingPath = tempFolder.getRoot().getAbsolutePath() + "/missing.bin";

        // Assert
        assertFalse(ModelStore.exists(missingPath));
    }
}
