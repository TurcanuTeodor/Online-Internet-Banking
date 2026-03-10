package ro.app.transaction.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OwnershipChecker {
    public void checkOwnership(JwtPrincipal principal, Long clientId) {
        if (!principal.isAdmin() && !clientId.equals(principal.clientId())) {
            throw new AccessDeniedException("Access denied: you can only access your own data");
        }
    }
}