package ro.app.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.app.auth.model.entity.User;
import ro.app.auth.repository.UserRepository;

/**
 * Server-to-server: revoke refresh tokens and disable login for a client (GDPR erasure).
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

    private void revokeAndDisable(User user) {
        refreshTokenService.revokeAllUserTokens(user);
        user.setEnabled(false);
        userRepository.save(user);
    }
}
