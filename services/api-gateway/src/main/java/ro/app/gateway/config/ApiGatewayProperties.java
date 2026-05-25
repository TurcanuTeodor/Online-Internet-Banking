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
public class ApiGatewayProperties {

    private Jwt jwt = new Jwt();

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

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }
}
