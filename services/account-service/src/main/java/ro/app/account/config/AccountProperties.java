package ro.app.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe externalized configuration for account service.
 * Consolidates JWT settings and service communication parameters.
 * 
 * Binds to properties prefixed with "app.account.*" in application.properties or application-*.yml
 */
@Component
@ConfigurationProperties(prefix = "app.account")
public class AccountProperties {

    private Jwt jwt = new Jwt();

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

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }
}
