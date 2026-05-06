package ro.app.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe externalized configuration for payment service.
 * Consolidates Stripe API keys, webhook settings, and payment processing parameters.
 * 
 * Binds to properties prefixed with "app.payment.*" in application.properties or application-*.yml
 */
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private Jwt jwt = new Jwt();
    private Stripe stripe = new Stripe();
    private Webhook webhook = new Webhook();
    private Processing processing = new Processing();
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

    public static class Stripe {
        /**
         * Stripe API key (test or live depending on environment)
         */
        private String apiKey;

        /**
         * Stripe webhook secret for signature verification
         */
        private String webhookSecret;

        /**
         * Max retries for Stripe API calls
         */
        private int maxRetries = 3;

        /**
         * Timeout in milliseconds for Stripe API calls
         */
        private long timeoutMs = 10000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Webhook {
        /**
         * Endpoint path for Stripe webhooks
         */
        private String path = "/webhooks/stripe";

        /**
         * Stripe webhook event types to process
         */
        private String[] enabledEvents = {
                "charge.succeeded",
                "charge.failed",
                "payment_intent.succeeded",
                "payment_intent.payment_failed"
        };

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String[] getEnabledEvents() {
            return enabledEvents;
        }

        public void setEnabledEvents(String[] enabledEvents) {
            this.enabledEvents = enabledEvents;
        }
    }

    public static class Processing {
        /**
         * Minimum payment amount (cents) to process
         */
        private int minAmountCents = 50;

        /**
         * Maximum payment amount (cents) per transaction
         */
        private int maxAmountCents = 99999999;

        /**
         * Payment processing timeout in milliseconds
         */
        private long processingTimeoutMs = 30000;

        public int getMinAmountCents() {
            return minAmountCents;
        }

        public void setMinAmountCents(int minAmountCents) {
            this.minAmountCents = minAmountCents;
        }

        public int getMaxAmountCents() {
            return maxAmountCents;
        }

        public void setMaxAmountCents(int maxAmountCents) {
            this.maxAmountCents = maxAmountCents;
        }

        public long getProcessingTimeoutMs() {
            return processingTimeoutMs;
        }

        public void setProcessingTimeoutMs(long processingTimeoutMs) {
            this.processingTimeoutMs = processingTimeoutMs;
        }
    }

    public static class Services {
        /**
         * Internal API secret for inter-service communication
         */
        private String internalApiSecret;

        /**
         * Transaction service URL for payment tracking
         */
        private String transactionServiceUrl;

        /**
         * Account service URL for account validation
         */
        private String accountServiceUrl;

        /**
         * Fraud service URL for payment evaluation
         */
        private String fraudServiceUrl;

        public String getInternalApiSecret() {
            return internalApiSecret;
        }

        public void setInternalApiSecret(String internalApiSecret) {
            this.internalApiSecret = internalApiSecret;
        }

        public String getTransactionServiceUrl() {
            return transactionServiceUrl;
        }

        public void setTransactionServiceUrl(String transactionServiceUrl) {
            this.transactionServiceUrl = transactionServiceUrl;
        }

        public String getAccountServiceUrl() {
            return accountServiceUrl;
        }

        public void setAccountServiceUrl(String accountServiceUrl) {
            this.accountServiceUrl = accountServiceUrl;
        }

        public String getFraudServiceUrl() {
            return fraudServiceUrl;
        }

        public void setFraudServiceUrl(String fraudServiceUrl) {
            this.fraudServiceUrl = fraudServiceUrl;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Stripe getStripe() {
        return stripe;
    }

    public void setStripe(Stripe stripe) {
        this.stripe = stripe;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }
}
