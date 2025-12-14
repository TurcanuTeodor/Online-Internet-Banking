package ro.app.banking.service.auth;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ro.app.banking.dto.auth.LoginRequest;
import ro.app.banking.dto.auth.LoginResponse;
import ro.app.banking.dto.auth.RegisterRequest;
import ro.app.banking.dto.auth.TwoFaSetupResponse;
import ro.app.banking.model.Client;
import ro.app.banking.model.Role;
import ro.app.banking.model.User;
import ro.app.banking.repository.ClientRepository;
import ro.app.banking.repository.UserRepository;
import ro.app.banking.security.jwt.JwtService;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final ClientRepository clientRepo;
    private PasswordEncoder encoder;
    private JwtService jwtService;
    private TotpService totpService;

    public AuthService(
            UserRepository userRepo,
            ClientRepository clientRepo,
            PasswordEncoder encoder,
            JwtService jwtService,
            TotpService totpService
    ) {
        this.userRepo = userRepo;
        this.clientRepo = clientRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.totpService = totpService;
    }

    public void register(RegisterRequest req){
        if(userRepo.existsByUsernameOrEmail(req.getUsernameOrEmail())){
            throw new RuntimeException("Username/Email already used");
        }

        Client client = clientRepo.findById(req.getClientId())
                                    .orElseThrow(() -> new RuntimeException("Client not found"));
                    
        User u = new User();
        u.setClient(client);
        u.setUsernameOrEmail(req.getUsernameOrEmail());
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setRole(Role.USER);
        userRepo.save(u);
    }

    public LoginResponse login(LoginRequest req){
        User user = userRepo.findByUsernameOrEmail(req.getUsernameOrEmail())
                            .orElseThrow(()-> new RuntimeException("Invalid credentials"));
        
        if(!encoder.matches(req.getPassword(), user.getPasswordHash())){
            throw new RuntimeException("Invalid credentials");
        }

        Long clientId= user.getClient().getId();

        //no active 2Fa -> jwt only
        if(!user.isTwoFactorEnabled()){
            String token = jwtService.generateToken(
                user.getUsernameOrEmail(),
                Map.of("role", user.getRole(), "clientId", clientId, "2fa", "ok")
            );

            return new LoginResponse(false, token, clientId, user.getRole().name());
        }

        //active 2Fa -> short temp token 
        String tempToken = jwtService.generateTempToken(
            user.getUsernameOrEmail(),
            Map.of("clientId", clientId, "2fa", "pending")
        );

        return new LoginResponse(true, tempToken, clientId, user.getRole().name());
    }

    //---------------2Fa Setup / Confirm / Verify-------------------------------

    public TwoFaSetupResponse setup2fa(String usernameOrEmail){
        User user = userRepo.findByUsernameOrEmail(usernameOrEmail)
                            .orElseThrow(()-> new RuntimeException("User not found"));
            
        String secret = totpService.generateSecret();
        String otpauth = totpService.buildOtpAuthUrl(user.getUsernameOrEmail(), secret);

        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false); //not confirmed yet
        userRepo.save(user);

        return new TwoFaSetupResponse(otpauth, secret);
    }

    

}
