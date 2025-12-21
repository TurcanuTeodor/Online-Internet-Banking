package ro.app.banking.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.app.banking.dto.auth.LoginRequest;
import ro.app.banking.dto.auth.LoginResponse;
import ro.app.banking.dto.auth.RegisterRequest;
import ro.app.banking.dto.auth.TwoFaConfirmRequest;
import ro.app.banking.dto.auth.TwoFaSetupResponse;
import ro.app.banking.dto.auth.TwoFaVerifyRequest;
import ro.app.banking.service.auth.AuthService;


@RestController
@Validated //enable method-level validation
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService= authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req){
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req){
        return ResponseEntity.ok(authService.login(req));
    }

    //---------------user needs to be logged in to activate 2fa-----------------

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFaSetupResponse> setup2fa(Authentication auth){
        return ResponseEntity.ok(authService.setup2fa(auth.getName()));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<?> confirm2fa(@Valid @RequestBody TwoFaConfirmRequest req, Authentication auth){
        authService.confirm2fa(auth.getName(), req.getCode());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verify2fa(@Valid @RequestBody TwoFaVerifyRequest req){
        return ResponseEntity.ok(authService.verify2fa(req.getTempToken(), req.getCode()));
    }
    
    
}
  