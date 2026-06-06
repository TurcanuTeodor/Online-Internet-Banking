package ro.app.fraud.tier3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Teste unitare pentru ReasoningBuilder (JUnit 4).
 *
 * Pattern testat: Builder (Creational) — construieste un mesaj de explicatie
 * al deciziei antifrauda pe baza scorului de anomalie si a importantelor (array).
 */
@RunWith(JUnit4.class)
public class ReasoningBuilderTest {

    @Test
    public void build_flaggedTransaction_containsPrimaryAndSecondaryFactors() {
        // Arrange
        double anomalyScore = 0.85;
        // Importances: amountRatio e maxim (0.8), hourSuspicion e al doilea (0.5)
        double[] importances = {0.8, 0.1, 0.5, 0.0, 0.2, 0.0};

        // Act
        String reasoning = ReasoningBuilder.build(true, anomalyScore, importances);

        // Assert
        assertTrue("Must contain the anomaly score", reasoning.contains("0.85"));
        assertTrue("Must mention the flagged transaction", reasoning.contains("flagged"));
        assertTrue("Primary factor: amountRatio", reasoning.contains("transaction amount"));
        assertTrue("Secondary factor: hourSuspicion", reasoning.contains("night-time hours"));
    }

    @Test
    public void build_normalTransaction_returnsNoRiskReasoning() {
        // Arrange
        double anomalyScore = 0.12;
        double[] importances = {0.01, 0.02, 0.05, 0.0, 0.01, 0.0};

        // Act
        String reasoning = ReasoningBuilder.build(false, anomalyScore, importances);

        // Assert
        assertTrue("Must contain the score", reasoning.contains("0.12"));
        assertTrue("Must mention the normal transaction", reasoning.contains("normal"));
        assertFalse("Must not mention primary factors", reasoning.contains("Primary factor"));
    }

    @Test
    public void build_emptyImportances_handlesZeroGracefully() {
        // Arrange — boundary: sum(importances) == 0 => evitam Impartire la 0 in procente
        double anomalyScore = 0.5;
        double[] importances = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        // Act
        String reasoning = ReasoningBuilder.build(true, anomalyScore, importances);

        // Assert
        assertTrue(reasoning.contains("0%")); // Procentul trebuie sa fie calculat ca 0%
    }
}
