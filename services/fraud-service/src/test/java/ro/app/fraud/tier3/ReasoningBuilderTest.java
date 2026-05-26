package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Teste unitare pentru ReasoningBuilder — generarea explicațiilor ML.
 */
class ReasoningBuilderTest {

    @Test
    void featureNames_has6Elements() {
        assertEquals(6, ReasoningBuilder.FEATURE_NAMES.length,
            "FEATURE_NAMES trebuie sa fie aliniat cu vectorul de 6 features");
    }

    @Test
    void build_flagged_containsPrimaryAndSecondaryFactor() {
        double[] importances = {0.05, 0.30, 0.10, 0.02, 0.50, 0.03};
        String reasoning = ReasoningBuilder.build(true, 0.75, importances);

        assertTrue(reasoning.contains("flagged as suspicious"), "Trebuie să menționeze 'flagged'");
        assertTrue(reasoning.contains("0.75"), "Trebuie să conțină anomaly score");
        // top feature = senderDepletionRatio (index 4, importance 0.50)
        assertTrue(reasoning.contains("sender account heavily or fully drained"),
            "Primary factor trebuie să fie senderDepletionRatio");
        // second feature = typeRisk (index 1, importance 0.30)
        assertTrue(reasoning.contains("high-risk transaction type"),
            "Secondary factor trebuie să fie typeRisk");
    }

    @Test
    void build_notFlagged_indicatesNormal() {
        double[] importances = {0.01, 0.01, 0.01, 0.01, 0.01, 0.01};
        String reasoning = ReasoningBuilder.build(false, 0.30, importances);

        assertTrue(reasoning.contains("considered normal"), "Trebuie să menționeze 'normal'");
        assertTrue(reasoning.contains("0.30"), "Trebuie să conțină anomaly score");
    }

    @Test
    void build_zeroImportances_noException() {
        double[] importances = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        // Nu ar trebui sa dea exceptie chiar daca toate importancele sunt 0
        assertDoesNotThrow(() -> ReasoningBuilder.build(true, 0.50, importances));
    }

    @Test
    void build_flagged_percentagesAddUp() {
        double[] importances = {0.1, 0.2, 0.3, 0.0, 0.0, 0.4};
        String reasoning = ReasoningBuilder.build(true, 0.80, importances);

        // Top = isRoundAmount (0.4/1.0 = 40%), Second = hourSuspicion (0.3/1.0 = 30%)
        assertTrue(reasoning.contains("40%"), "Top percentage should be ~40%");
        assertTrue(reasoning.contains("30%"), "Second percentage should be ~30%");
    }
}
