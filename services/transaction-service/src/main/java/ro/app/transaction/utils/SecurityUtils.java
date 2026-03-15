package ro.app.transaction.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import ro.app.transaction.security.JwtPrincipal;

public class SecurityUtils {

    public static Long getClientIdFromJwt(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.clientId();
        }
        throw new IllegalStateException("clientId not found in principal");
    }

    public static String getRoleFromJwt(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.role();
        }
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("");
    }

    public static boolean isAdmin(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.isAdmin();
        }
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().contains("ADMIN"));
    }
}

