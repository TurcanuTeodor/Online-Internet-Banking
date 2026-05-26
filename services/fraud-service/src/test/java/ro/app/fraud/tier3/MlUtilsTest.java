package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Teste unitare pentru MlUtils — statistici și MinMaxScaler.
 * Acoperă: computeMeans, computeMins, computeMaxes, minMaxScale, minMaxScaleSingle, argmax.
 */
class MlUtilsTest {

    // ── computeMeans ────────────────────────────────────────────────────────

    @Test
    void computeMeans_standardCase() {
        double[][] data = {
            {1.0, 4.0},
            {3.0, 6.0},
            {2.0, 8.0}
        };
        double[] means = MlUtils.computeMeans(data);

        assertEquals(2, means.length);
        assertEquals(2.0, means[0], 1e-9, "mean col0: (1+3+2)/3 = 2.0");
        assertEquals(6.0, means[1], 1e-9, "mean col1: (4+6+8)/3 = 6.0");
    }

    @Test
    void computeMeans_emptyData_returnsEmpty() {
        double[][] data = {};
        double[] means = MlUtils.computeMeans(data);
        assertEquals(0, means.length);
    }

    @Test
    void computeMeans_singleRow() {
        double[][] data = {{5.0, 10.0, 15.0}};
        double[] means = MlUtils.computeMeans(data);
        assertEquals(5.0, means[0], 1e-9);
        assertEquals(10.0, means[1], 1e-9);
        assertEquals(15.0, means[2], 1e-9);
    }

    // ── computeMins / computeMaxes ──────────────────────────────────────────

    @Test
    void computeMins_standardCase() {
        double[][] data = {
            {1.0, 9.0, 3.0},
            {5.0, 2.0, 7.0},
            {3.0, 6.0, 1.0}
        };
        double[] mins = MlUtils.computeMins(data);
        assertEquals(1.0, mins[0], 1e-9);
        assertEquals(2.0, mins[1], 1e-9);
        assertEquals(1.0, mins[2], 1e-9);
    }

    @Test
    void computeMaxes_standardCase() {
        double[][] data = {
            {1.0, 9.0, 3.0},
            {5.0, 2.0, 7.0},
            {3.0, 6.0, 1.0}
        };
        double[] maxes = MlUtils.computeMaxes(data);
        assertEquals(5.0, maxes[0], 1e-9);
        assertEquals(9.0, maxes[1], 1e-9);
        assertEquals(7.0, maxes[2], 1e-9);
    }

    @Test
    void computeMins_emptyData_returnsEmpty() {
        assertEquals(0, MlUtils.computeMins(new double[][]{}).length);
    }

    @Test
    void computeMaxes_emptyData_returnsEmpty() {
        assertEquals(0, MlUtils.computeMaxes(new double[][]{}).length);
    }

    // ── minMaxScaleSingle ──────────────────────────────────────────────────

    @Test
    void minMaxScaleSingle_standardScaling() {
        double[] row = {2.0, 5.0, 10.0};
        double[] mins = {0.0, 0.0, 0.0};
        double[] maxes = {4.0, 10.0, 20.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);
        assertEquals(0.5, scaled[0], 1e-9, "2/4 = 0.5");
        assertEquals(0.5, scaled[1], 1e-9, "5/10 = 0.5");
        assertEquals(0.5, scaled[2], 1e-9, "10/20 = 0.5");
    }

    @Test
    void minMaxScaleSingle_clampsToZeroAndOne() {
        // Out-of-distribution: valori mai mici ca min si mai mari ca max
        double[] row = {-5.0, 25.0};
        double[] mins = {0.0, 0.0};
        double[] maxes = {10.0, 20.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);
        assertEquals(0.0, scaled[0], 1e-9, "Valoare sub min → clamped la 0.0");
        assertEquals(1.0, scaled[1], 1e-9, "Valoare peste max → clamped la 1.0");
    }

    @Test
    void minMaxScaleSingle_constantFeature_returnsZero() {
        // Daca min == max (feature constant), result = 0.0
        double[] row = {5.0};
        double[] mins = {5.0};
        double[] maxes = {5.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);
        assertEquals(0.0, scaled[0], 1e-9, "Feature constant → 0.0");
    }

    @Test
    void minMaxScaleSingle_atMinReturnsZero_atMaxReturnsOne() {
        double[] row1 = {0.0, 10.0};
        double[] row2 = {10.0, 0.0};
        double[] mins = {0.0, 0.0};
        double[] maxes = {10.0, 10.0};

        double[] scaled1 = MlUtils.minMaxScaleSingle(row1, mins, maxes);
        assertEquals(0.0, scaled1[0], 1e-9, "La min → 0.0");
        assertEquals(1.0, scaled1[1], 1e-9, "La max → 1.0");

        double[] scaled2 = MlUtils.minMaxScaleSingle(row2, mins, maxes);
        assertEquals(1.0, scaled2[0], 1e-9, "La max → 1.0");
        assertEquals(0.0, scaled2[1], 1e-9, "La min → 0.0");
    }

    // ── minMaxScale (matrice) ───────────────────────────────────────────────

    @Test
    void minMaxScale_matrix_allRowsScaled() {
        double[][] data = {
            {0.0, 10.0},
            {5.0, 0.0},
            {10.0, 5.0}
        };
        double[] mins = {0.0, 0.0};
        double[] maxes = {10.0, 10.0};

        double[][] scaled = MlUtils.minMaxScale(data, mins, maxes);

        assertEquals(3, scaled.length);
        assertEquals(0.0, scaled[0][0], 1e-9);
        assertEquals(1.0, scaled[0][1], 1e-9);
        assertEquals(0.5, scaled[1][0], 1e-9);
        assertEquals(0.0, scaled[1][1], 1e-9);
        assertEquals(1.0, scaled[2][0], 1e-9);
        assertEquals(0.5, scaled[2][1], 1e-9);
    }

    @Test
    void minMaxScale_emptyMatrix_returnsEmpty() {
        double[][] scaled = MlUtils.minMaxScale(new double[][]{}, new double[]{}, new double[]{});
        assertEquals(0, scaled.length);
    }

    // ── argmax ──────────────────────────────────────────────────────────────

    @Test
    void argmax_returnsIndexOfMaxValue() {
        assertEquals(2, MlUtils.argmax(new double[]{1.0, 3.0, 5.0, 2.0}));
    }

    @Test
    void argmax_firstMaxWhenTied() {
        assertEquals(0, MlUtils.argmax(new double[]{5.0, 5.0, 3.0}));
    }

    @Test
    void argmax2_returnsSecondHighest() {
        assertEquals(1, MlUtils.argmax2(new double[]{1.0, 3.0, 5.0, 2.0}));
    }
}
