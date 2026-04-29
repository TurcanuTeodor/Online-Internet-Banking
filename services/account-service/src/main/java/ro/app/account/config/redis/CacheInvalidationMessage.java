package ro.app.account.config.redis;

public record CacheInvalidationMessage(
        String cacheName,
        String key,
        String reason) {
}