package ro.app.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe externalized configuration for API Gateway service.
 * Consolidates JWT settings, service routing parameters, and downstream service URLs.
 * 
 * Binds to properties prefixed with "app.gateway.*" and "services.*" in application.properties or application-*.yml
 */
@Component
@ConfigurationProperties(prefix = "app.gateway")
public class GatewayProperties {

    private Jwt jwt = new Jwt();
    private Services services = new Services();

    public static class Jwt {
        /**
         * JWT signing secret key for token validation
         */
        private String secret;

        /**
         * JWT issuer identifier for verification
         */
        private String issuer;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Services {
        /**
         * Authentication service URL
         */
        private String authUrl = "http://localhost:8081";

        /**
         * Client service URL
         */
        private String clientUrl = "http://localhost:8082";

        /**
         * Account service URL
         */
        private String accountUrl = "http://localhost:8083";

        /**
         * Transaction service URL
         */
        private String transactionUrl = "http://localhost:8084";

        /**
         * Payment service URL
         */
        private String paymentUrl = "http://localhost:8085";

        /**
         * Fraud service URL
         */
        private String fraudUrl = "http://localhost:8086";

        public String getAuthUrl() {
            return authUrl;
        }

        public void setAuthUrl(String authUrl) {
            this.authUrl = authUrl;
        }

        public String getClientUrl() {
            return clientUrl;
        }

        public void setClientUrl(String clientUrl) {
            this.clientUrl = clientUrl;
        }

        public String getAccountUrl() {
            return accountUrl;
        }

        public void setAccountUrl(String accountUrl) {
            this.accountUrl = accountUrl;
        }

        public String getTransactionUrl() {
            return transactionUrl;
        }

        public void setTransactionUrl(String transactionUrl) {
            this.transactionUrl = transactionUrl;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public void setPaymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
        }

        public String getFraudUrl() {
            return fraudUrl;
        }

        public void setFraudUrl(String fraudUrl) {
            this.fraudUrl = fraudUrl;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }
}
