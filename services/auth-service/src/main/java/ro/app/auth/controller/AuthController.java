package ro.app.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ro.app.auth.dto.auth.LoginRequest;
import ro.app.auth.dto.auth.LoginResponse;
import ro.app.auth.dto.auth.RegisterRequest;
import ro.app.auth.dto.token.RefreshTokenRequest;
import ro.app.auth.dto.token.RefreshTokenResponse;
import ro.app.auth.dto.twofa.TwoFaConfirmRequest;
import ro.app.auth.dto.twofa.TwoFaSetupResponse;
import ro.app.auth.dto.twofa.TwoFaVerifyRequest;
import ro.app.auth.audit.AuditService;
import ro.app.auth.service.AuthService;
import ro.app.auth.service.RateLimitService;
import ro.app.auth.service.TokenService;
import ro.app.auth.service.TwoFaService;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TwoFaService twoFaService;
    private final TokenService tokenService;
    private final RateLimitService rateLimitService;
    private final AuditService auditService;

    public AuthController(
            AuthService authService,
            TwoFaService twoFaService,
            TokenService tokenService,
            RateLimitService rateLimitService,
            AuditService auditService) {
        this.authService = authService;
        this.twoFaService = twoFaService;
        this.tokenService = tokenService;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String clientIp = httpReq.getRemoteAddr();
        rateLimitService.validateLoginAttempt(clientIp);
        try {
            LoginResponse response = authService.login(req);
            rateLimitService.recordSuccessfulAttempt(clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            rateLimitService.recordFailedAttempt(clientIp);
            auditService.log("LOGIN_FAILED", null, null, null, "ip=" + clientIp + " reason=" + ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFaSetupResponse> setup2fa(Authentication auth) {
        return ResponseEntity.ok(twoFaService.setup2fa(auth.getName()));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<?> confirm2fa(@Valid @RequestBody TwoFaConfirmRequest req, Authentication auth) {
        twoFaService.confirm2fa(auth.getName(), req.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verify2fa(@Valid @RequestBody TwoFaVerifyRequest req) {
        return ResponseEntity.ok(twoFaService.verify2fa(req.getTempToken(), req.getCode()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(tokenService.refreshToken(req.getRefreshToken(), req.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ro.app.auth.dto.auth.ChangePasswordRequest req,
            Authentication auth) {
        authService.changePassword(auth.getName(), req);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please log in again."));
    }
}
