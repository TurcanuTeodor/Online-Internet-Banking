package ro.app.client.security;

public record JwtPrincipal(String username, Long clientId, String role, String encryptionKey) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
