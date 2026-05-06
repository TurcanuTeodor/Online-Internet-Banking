package ro.app.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe externalized configuration for fraud detection service.
 * Consolidates tier thresholds, ML model parameters, and async processing settings.
 * 
 * Binds to properties prefixed with "fraud.*" in application.properties or application-*.yml
 */
@Component
@ConfigurationProperties(prefix = "fraud")
public class FraudProperties {

    private Jwt jwt = new Jwt();
    private Tier1 tier1 = new Tier1();
    private Tier2 tier2 = new Tier2();
    private Tier3 tier3 = new Tier3();
    private Services services = new Services();

    public static class Jwt {
        /**
         * JWT signing secret key for token validation
         */
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Tier1 {
        /**
         * Large transaction threshold (euros) that triggers STEP_UP
         */
        private double largeAmountThreshold = 10000.0;

        /**
         * New account minimum age (days) before full scoring
         */
        private int newAccountAgeDays = 30;

        /**
         * Burst limit: max fraud evaluations per account in 60 seconds
         */
        private int burstLimit = 5;

        /**
         * Burst time window in seconds
         */
        private int burstWindowSeconds = 60;

        public double getLargeAmountThreshold() {
            return largeAmountThreshold;
        }

        public void setLargeAmountThreshold(double largeAmountThreshold) {
            this.largeAmountThreshold = largeAmountThreshold;
        }

        public int getNewAccountAgeDays() {
            return newAccountAgeDays;
        }

        public void setNewAccountAgeDays(int newAccountAgeDays) {
            this.newAccountAgeDays = newAccountAgeDays;
        }

        public int getBurstLimit() {
            return burstLimit;
        }

        public void setBurstLimit(int burstLimit) {
            this.burstLimit = burstLimit;
        }

        public int getBurstWindowSeconds() {
            return burstWindowSeconds;
        }

        public void setBurstWindowSeconds(int burstWindowSeconds) {
            this.burstWindowSeconds = burstWindowSeconds;
        }
    }

    public static class Tier2 {
        /**
         * Lower threshold: score < this = ALLOW
         */
        private double lowerThreshold = 30.0;

        /**
         * Upper threshold: score >= this = FLAG
         */
        private double upperThreshold = 70.0;

        /**
         * Timeout for Tier2 async execution (milliseconds)
         */
        private long timeoutMs = 500;

        public double getLowerThreshold() {
            return lowerThreshold;
        }

        public void setLowerThreshold(double lowerThreshold) {
            this.lowerThreshold = lowerThreshold;
        }

        public double getUpperThreshold() {
            return upperThreshold;
        }

        public void setUpperThreshold(double upperThreshold) {
            this.upperThreshold = upperThreshold;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Tier3 {
        /**
         * ML model enabled/disabled flag
         */
        private boolean mlEnabled = true;

        /**
         * Isolation Forest contamination parameter (fraction of anomalies in training)
         */
        private double mlContamination = 0.05;

        /**
         * Random seed for reproducible data generation and model training
         */
        private int mlSeed = 42;

        /**
         * Anomaly score decision threshold (range: 0.0-1.0)
         */
        private double mlThreshold = 0.62;

        /**
         * Number of synthetic training samples
         */
        private int mlTrainingSamples = 1000;

        /**
         * Timeout for Tier3 ML analysis (milliseconds)
         */
        private long timeoutMs = 5000;

        /**
         * Thread pool size for Tier3 executor
         */
        private int threadPoolSize = 2;

        public boolean isMlEnabled() {
            return mlEnabled;
        }

        public void setMlEnabled(boolean mlEnabled) {
            this.mlEnabled = mlEnabled;
        }

        public double getMlContamination() {
            return mlContamination;
        }

        public void setMlContamination(double mlContamination) {
            this.mlContamination = mlContamination;
        }

        public int getMlSeed() {
            return mlSeed;
        }

        public void setMlSeed(int mlSeed) {
            this.mlSeed = mlSeed;
        }

        public double getMlThreshold() {
            return mlThreshold;
        }

        public void setMlThreshold(double mlThreshold) {
            this.mlThreshold = mlThreshold;
        }

        public int getMlTrainingSamples() {
            return mlTrainingSamples;
        }

        public void setMlTrainingSamples(int mlTrainingSamples) {
            this.mlTrainingSamples = mlTrainingSamples;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public static class Services {
        /**
         * Internal API secret for inter-service communication
         */
        private String internalApiSecret;

        /**
         * Account service URL
         */
        private String accountServiceUrl;

        /**
         * Transaction service URL
         */
        private String transactionServiceUrl;

        public String getInternalApiSecret() {
            return internalApiSecret;
        }

        public void setInternalApiSecret(String internalApiSecret) {
            this.internalApiSecret = internalApiSecret;
        }

        public String getAccountServiceUrl() {
            return accountServiceUrl;
        }

        public void setAccountServiceUrl(String accountServiceUrl) {
            this.accountServiceUrl = accountServiceUrl;
        }

        public String getTransactionServiceUrl() {
            return transactionServiceUrl;
        }

        public void setTransactionServiceUrl(String transactionServiceUrl) {
            this.transactionServiceUrl = transactionServiceUrl;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Tier1 getTier1() {
        return tier1;
    }

    public void setTier1(Tier1 tier1) {
        this.tier1 = tier1;
    }

    public Tier2 getTier2() {
        return tier2;
    }

    public void setTier2(Tier2 tier2) {
        this.tier2 = tier2;
    }

    public Tier3 getTier3() {
        return tier3;
    }

    public void setTier3(Tier3 tier3) {
        this.tier3 = tier3;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }
}
