package ro.app.banking.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Service
public class TotpService {
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider(); //current server time
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(); //generator calculates the totp = secret+time
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider); //verifier req codeGen to calc code, compares with the user's and tracks time

    @Value("${app.2fa.app-name}") 
    private String appName; //this name appears in Google Auth

    public String generateSecret(){ //called when user turns on 2FA
        return secretGenerator.generate(); //random key storeed in db, used for code verif
    }

    //building the url for qr code
    public String buildOtpAuthUrl(String email, String secret){
        QrData data = new QrData.Builder()
                                .label(email)
                                .secret(secret)
                                .issuer(appName)
                                .algorithm(HashingAlgorithm.SHA1)
                                .digits(6)
                                .period(30)
                                .build();
        return data.getUri(); //otpauth://totp/Online...
    }

    public boolean verifyCode(String secret, String code){ //called when user writes 2Fa code
        return codeVerifier.isValidCode(secret, code);
    }

}
