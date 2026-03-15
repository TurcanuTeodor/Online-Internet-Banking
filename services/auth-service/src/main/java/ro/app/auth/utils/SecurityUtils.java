package ro.app.auth.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import ro.app.auth.security.UserPrincipal;

public class SecurityUtils {

    public static Long getClientIdFromUserPrincipal(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getClientId();
        }
        throw new IllegalStateException("clientId not found in principal");
    }

    public static String getRoleFromAuth(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("");
    }

    public static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().contains("ADMIN"));
    }
}

