package ro.app.fraud.tier3;

import java.util.Random;

public final class TrainingDataGenerator {
    
    private TrainingDataGenerator() {
        // Prevent instantiation
    }

    public static double[][] generate(int normalCount, int anomalyCount, long seed) {
        Random random = new Random(seed);
        double[][] data = new double[normalCount + anomalyCount][6]; // 6 features

        // --- NORMAL transactions ---
        // Amounts follow a log-normal distribution (ECB-confirmed pattern for retail banking)
        // Slightly higher mean (5.8) to reflect that real "normal" includes some large
        // but legitimate transfers (salaries, rent, etc.).
        for (int i = 0; i < normalCount; i++) {
            double logAmount = 5.8 + 1.5 * random.nextGaussian();          // ~mean 330 RON, wider spread
            data[i][0] = Math.min(1.0, Math.exp(logAmount) / 5000.0);      // amount_ratio
            data[i][1] = Math.max(0, Math.min(1.0, 0.25 + 0.15 * random.nextGaussian())); // tier2_score (low)
            data[i][2] = Math.max(0, Math.min(1.0, 0.1  + 0.1 * random.nextGaussian())); // frequency_24h (low)
            data[i][3] = random.nextDouble() < 0.2 ? 1.0 : 0.0;            // new_recipient: 20% normal
            data[i][4] = Math.max(0, Math.min(1.0, 0.1  + 0.1 * random.nextGaussian())); // hour_deviation (low)
            data[i][5] = random.nextDouble() < 0.05 ? 1.0 : 0.0;           // new_account: 5% normal
        }

        // --- ANOMALY transactions (sophisticated fraud) ---
        // Reduced contrast vs normal data — anomalies overlap with normal distribution
        // This forces the Isolation Forest to learn subtle patterns rather than obvious outliers,
        // resulting in a more realistic and academically credible model.
        for (int i = normalCount; i < normalCount + anomalyCount; i++) {
            // Corelație: recipient nou
            double isNewRecipient = random.nextDouble() < 0.6 ? 1.0 : 0.0;
            data[i][3] = isNewRecipient;
            
            // Corelație: sumele sunt mai mari când recipientul e nou
            double baseAmount = isNewRecipient == 1.0 ? 6.8 : 5.5;
            double logAmount = baseAmount + 1.8 * random.nextGaussian();
            data[i][0] = Math.min(1.0, Math.exp(logAmount) / 5000.0);      // amount_ratio
            
            // Corelație: ora e mai deviantă când suma e mare
            double amountHighMultiplier = data[i][0] > 0.6 ? 0.5 : 0.2;
            data[i][4] = Math.max(0, Math.min(1.0, amountHighMultiplier + 0.2 * random.nextGaussian())); // hour_deviation

            // Restul rămân similare cu ce aveai:
            data[i][1] = Math.max(0, Math.min(1.0, 0.4 + 0.3 * random.nextGaussian())); // tier2_score
            data[i][2] = Math.max(0, Math.min(1.0, 0.35 + 0.2 * random.nextGaussian())); // frequency_24h
            data[i][5] = random.nextDouble() < 0.5 ? 1.0 : 0.0;            // new_account: 50% fraud
        }

        return data;
    }
}
