package ro.app.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe externalized configuration for authentication service.
 * Consolidates JWT settings, Redis cache parameters, and rate limiting.
 * 
 * Binds to properties prefixed with "app.jwt.*", "app.auth.*", and "app.redis.*" 
 * in application.properties or application-*.yml
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Redis redis = new Redis();
    private RateLimit rateLimit = new RateLimit();
    private Services services = new Services();
    private Encryption encryption = new Encryption();

    public static class Jwt {
        /**
         * JWT signing secret key (should be at least 256 bits for HS256)
         */
        private String secret;

        /**
         * JWT token expiration time in minutes
         */
        private long expirationMinutes = 60;

        /**
         * Temporary token (step-up auth) expiration in minutes
         */
        private long tempExpirationMinutes = 15;

        /**
         * JWT issuer identifier
         */
        private String issuer = "online-banking-app";

        /**
         * Refresh token validity in days
         */
        private long refreshTokenDays = 7;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }

        public long getTempExpirationMinutes() {
            return tempExpirationMinutes;
        }

        public void setTempExpirationMinutes(long tempExpirationMinutes) {
            this.tempExpirationMinutes = tempExpirationMinutes;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getRefreshTokenDays() {
            return refreshTokenDays;
        }

        public void setRefreshTokenDays(long refreshTokenDays) {
            this.refreshTokenDays = refreshTokenDays;
        }
    }

    public static class Redis {
        /**
         * Redis hostname/IP
         */
        private String host = "localhost";

        /**
         * Redis port
         */
        private int port = 6379;

        /**
         * Redis password (if required)
         */
        private String password;

        /**
         * Redis database index for JWT blacklist
         */
        private int database = 1;

        /**
         * Connection timeout in milliseconds
         */
        private long timeoutMs = 5000;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class RateLimit {
        /**
         * Maximum login attempts before rate limiting
         */
        private int maxLoginAttempts = 5;

        /**
         * Rate limit window in seconds
         */
        private int windowSeconds = 300;

        /**
         * Account lockout duration in minutes
         */
        private long lockoutMinutes = 15;

        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }

        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public long getLockoutMinutes() {
            return lockoutMinutes;
        }

        public void setLockoutMinutes(long lockoutMinutes) {
            this.lockoutMinutes = lockoutMinutes;
        }
    }

    public static class Services {
        /**
         * Internal API secret for inter-service communication
         */
        private String internalApiSecret;

        /**
         * Client service URL
         */
        private String clientServiceUrl = "http://client-service:8082";

        public String getInternalApiSecret() {
            return internalApiSecret;
        }

        public void setInternalApiSecret(String internalApiSecret) {
            this.internalApiSecret = internalApiSecret;
        }

        public String getClientServiceUrl() {
            return clientServiceUrl;
        }

        public void setClientServiceUrl(String clientServiceUrl) {
            this.clientServiceUrl = clientServiceUrl;
        }
    }

    public static class Encryption {
        /**
         * Legacy encryption key for backwards compatibility with old database rows
         */
        private String legacyKey;

        public String getLegacyKey() {
            return legacyKey;
        }

        public void setLegacyKey(String legacyKey) {
            this.legacyKey = legacyKey;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }
}
