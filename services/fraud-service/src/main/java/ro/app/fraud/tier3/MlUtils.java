package ro.app.fraud.tier3;

/**
 * Utilitare ML pentru calcule statistice si scalare.
 *
 * Metode adaugate (FIX #3 — MinMaxScaler):
 * computeMins / computeMaxes — calculeaza limitele pe setul de train
 * minMaxScale (matrice) — scaleaza un set intreg de date
 * minMaxScaleSingle (vector) — scaleaza un singur vector la inferenta live
 *
 * IMPORTANT: mins/maxes sunt calculate EXCLUSIV pe setul de TRAIN, nu pe
 * intregul
 * dataset. Aplicarea lor identica la inferenta este esentiala pentru
 * corectitudinea
 * modelului (train/inference parity).
 */
public final class MlUtils {

    private MlUtils() {
    }

    // Statistici

    /** Calculeaza media fiecarei coloane (feature) pe intregul dataset. */
    public static double[] computeMeans(double[][] data) {
        if (data.length == 0)
            return new double[0];
        double[] means = new double[data[0].length];
        for (double[] row : data) {
            for (int j = 0; j < row.length; j++) {
                means[j] += row[j];
            }
        }
        for (int j = 0; j < means.length; j++) {
            means[j] /= data.length;
        }
        return means;
    }

    // MinMaxScaler

    /**
     * Calculeaza valorile minime per feature pe setul de train.
     * Trebuie salvate in ModelSnapshot pentru a fi aplicate identic la inferenta.
     */
    public static double[] computeMins(double[][] data) {
        if (data.length == 0)
            return new double[0];
        int nFeatures = data[0].length;
        double[] mins = new double[nFeatures];
        for (int j = 0; j < nFeatures; j++)
            mins[j] = Double.MAX_VALUE;
        for (double[] row : data) {
            for (int j = 0; j < nFeatures; j++) {
                if (row[j] < mins[j])
                    mins[j] = row[j];
            }
        }
        return mins;
    }

    /**
     * Calculeaza valorile maxime per feature pe setul de train.
     * Trebuie salvate in ModelSnapshot pentru a fi aplicate identic la inferenta.
     */
    public static double[] computeMaxes(double[][] data) {
        if (data.length == 0)
            return new double[0];
        int nFeatures = data[0].length;
        double[] maxes = new double[nFeatures];
        for (int j = 0; j < nFeatures; j++)
            maxes[j] = -Double.MAX_VALUE;
        for (double[] row : data) {
            for (int j = 0; j < nFeatures; j++) {
                if (row[j] > maxes[j])
                    maxes[j] = row[j];
            }
        }
        return maxes;
    }

    /**
     * Aplica MinMaxScaling pe intreaga matrice de date (pentru antrenament).
     *
     * Formula: scaled = (x - min) / (max - min)
     * Daca max == min (feature constant) → scaled = 0.0 (evita impartire la zero).
     */
    public static double[][] minMaxScale(double[][] data, double[] mins, double[] maxes) {
        int n = data.length;
        if (n == 0)
            return new double[0][0];
        int f = data[0].length;
        double[][] scaled = new double[n][f];
        for (int i = 0; i < n; i++) {
            scaled[i] = minMaxScaleSingle(data[i], mins, maxes);
        }
        return scaled;
    }

    /**
     * Aplica MinMaxScaling pe un singur vector (pentru inferenta live).
     * Foloseste aceleasi mins/maxes salvate la antrenament (din ModelSnapshot).
     */
    public static double[] minMaxScaleSingle(double[] row, double[] mins, double[] maxes) {
        double[] scaled = new double[row.length];
        for (int j = 0; j < row.length; j++) {
            double range = maxes[j] - mins[j];
            // Daca range == 0 (feature constant), setam la 0.0 — feature fara informatie
            scaled[j] = (range < 1e-10) ? 0.0 : (row[j] - mins[j]) / range;
            // Clamp la [0, 1] pentru valori out-of-distribution la inferenta
            scaled[j] = Math.max(0.0, Math.min(1.0, scaled[j]));
        }
        return scaled;
    }

    // ── Argmax ────────────────────────────────────────────────────────────────

    public static int argmax(double[] arr) {
        int idx = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > arr[idx]) {
                idx = i;
            }
        }
        return idx;
    }

    public static int argmax2(double[] arr) {
        int first = argmax(arr), second = (first == 0) ? 1 : 0;
        for (int i = 0; i < arr.length; i++) {
            if (i != first && arr[i] > arr[second]) {
                second = i;
            }
        }
        return second;
    }
}
