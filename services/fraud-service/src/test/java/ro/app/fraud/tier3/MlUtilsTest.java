package ro.app.fraud.tier3;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Teste unitare pentru MlUtils (JUnit 4).
 *
 * Acopera: computeMeans, computeMins, computeMaxes, minMaxScale,
 *           minMaxScaleSingle, argmax, argmax2.
 *
 * Nu avem nevoie de Mockito — MlUtils este o clasa utilitara statica.
 * Tehnici aplicate: BVA (matrice vida, rand singular), echivalenta de clase.
 */
@RunWith(JUnit4.class)
public class MlUtilsTest {

    // ── computeMeans ──────────────────────────────────────────────────────────

    @Test
    public void computeMeans_standardCase_returnsCorrectMeans() {
        double[][] data = {
            {1.0, 4.0},
            {3.0, 6.0},
            {2.0, 8.0}
        };
        double[] means = MlUtils.computeMeans(data);

        assertEquals(2, means.length);
        assertEquals("mean col0: (1+3+2)/3 = 2.0", 2.0, means[0], 1e-9);
        assertEquals("mean col1: (4+6+8)/3 = 6.0", 6.0, means[1], 1e-9);
    }

    @Test
    public void computeMeans_emptyData_returnsEmptyArray() {
        // Boundary: matrice vida
        double[] means = MlUtils.computeMeans(new double[][]{});
        assertEquals(0, means.length);
    }

    @Test
    public void computeMeans_singleRow_returnsThatRowValues() {
        double[][] data = {{5.0, 10.0, 15.0}};
        double[] means = MlUtils.computeMeans(data);

        assertEquals(5.0,  means[0], 1e-9);
        assertEquals(10.0, means[1], 1e-9);
        assertEquals(15.0, means[2], 1e-9);
    }

    // ── computeMins / computeMaxes ─────────────────────────────────────────

    @Test
    public void computeMins_standardCase_returnsMinPerColumn() {
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
    public void computeMaxes_standardCase_returnsMaxPerColumn() {
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
    public void computeMins_emptyData_returnsEmptyArray() {
        assertEquals(0, MlUtils.computeMins(new double[][]{}).length);
    }

    @Test
    public void computeMaxes_emptyData_returnsEmptyArray() {
        assertEquals(0, MlUtils.computeMaxes(new double[][]{}).length);
    }

    // ── minMaxScaleSingle ──────────────────────────────────────────────────

    @Test
    public void minMaxScaleSingle_standardScaling_returnsHalf() {
        double[] row   = {2.0, 5.0, 10.0};
        double[] mins  = {0.0, 0.0, 0.0};
        double[] maxes = {4.0, 10.0, 20.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);

        assertEquals("2/4 = 0.5",  0.5, scaled[0], 1e-9);
        assertEquals("5/10 = 0.5", 0.5, scaled[1], 1e-9);
        assertEquals("10/20 = 0.5",0.5, scaled[2], 1e-9);
    }

    @Test
    public void minMaxScaleSingle_belowMin_clampedToZero() {
        // Boundary: valoare sub min → 0.0
        double[] row   = {-5.0};
        double[] mins  = {0.0};
        double[] maxes = {10.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);

        assertEquals("below min → 0.0", 0.0, scaled[0], 1e-9);
    }

    @Test
    public void minMaxScaleSingle_aboveMax_clampedToOne() {
        // Boundary: valoare peste max → 1.0
        double[] row   = {25.0};
        double[] mins  = {0.0};
        double[] maxes = {20.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);

        assertEquals("above max → 1.0", 1.0, scaled[0], 1e-9);
    }

    @Test
    public void minMaxScaleSingle_constantFeature_returnsZero() {
        // Boundary: min == max (feature constant) → 0.0
        double[] row   = {5.0};
        double[] mins  = {5.0};
        double[] maxes = {5.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);

        assertEquals("constant feature → 0.0", 0.0, scaled[0], 1e-9);
    }

    @Test
    public void minMaxScaleSingle_atMin_returnsZero() {
        double[] row   = {0.0};
        double[] mins  = {0.0};
        double[] maxes = {10.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);

        assertEquals("at min → 0.0", 0.0, scaled[0], 1e-9);
    }

    @Test
    public void minMaxScaleSingle_atMax_returnsOne() {
        double[] row   = {10.0};
        double[] mins  = {0.0};
        double[] maxes = {10.0};

        double[] scaled = MlUtils.minMaxScaleSingle(row, mins, maxes);

        assertEquals("at max → 1.0", 1.0, scaled[0], 1e-9);
    }

    // ── minMaxScale (matrice) ─────────────────────────────────────────────

    @Test
    public void minMaxScale_matrix_allRowsScaled() {
        double[][] data = {
            {0.0, 10.0},
            {5.0,  0.0},
            {10.0, 5.0}
        };
        double[] mins  = {0.0, 0.0};
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
    public void minMaxScale_emptyMatrix_returnsEmpty() {
        double[][] scaled = MlUtils.minMaxScale(new double[][]{}, new double[]{}, new double[]{});
        assertEquals(0, scaled.length);
    }

    // ── argmax / argmax2 ──────────────────────────────────────────────────

    @Test
    public void argmax_returnsIndexOfMaxValue() {
        assertEquals(2, MlUtils.argmax(new double[]{1.0, 3.0, 5.0, 2.0}));
    }

    @Test
    public void argmax_tiedValues_returnsFirstIndex() {
        // Boundary: tie → returneaza primul index
        assertEquals(0, MlUtils.argmax(new double[]{5.0, 5.0, 3.0}));
    }

    @Test
    public void argmax2_returnsIndexOfSecondHighestValue() {
        assertEquals(1, MlUtils.argmax2(new double[]{1.0, 3.0, 5.0, 2.0}));
    }
}
