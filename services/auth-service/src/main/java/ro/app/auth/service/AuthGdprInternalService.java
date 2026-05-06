package ro.app.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.auth.model.entity.User;
import ro.app.auth.repository.UserRepository;

/**
 * Server-to-server: revoke refresh tokens, disable login, and erase 2FA secret for a client (GDPR erasure).
 */
@Service
public class AuthGdprInternalService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthGdprInternalService(UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void deactivateUserForGdpr(Long clientId) {
        userRepository.findByClientId(clientId).ifPresent(this::revokeAndDisable);
    }

    /**
     * GDPR Art. 17 — Right to erasure.
     * Revokes all sessions, disables the account, and wipes the TOTP secret.
     * The TOTP secret is considered PII (unique cryptographic identifier per user)
     * and must be erased along with other personal data.
     */
    private void revokeAndDisable(User user) {
        refreshTokenService.revokeAllUserTokens(user);
        user.setEnabled(false);
        // Erase TOTP secret — PII under GDPR Art. 17
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
    }
}
