package ro.app.payment.security;

public record JwtPrincipal(String username, Long clientId, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
