package ro.app.banking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TwoFaVerifyRequest {
    @NotBlank(message = "Temporary token is required")
    private String tempToken;
    
    @NotBlank(message = "2FA code is required")
    @Size(min = 6, max = 6, message = "2FA code must be exactly 6 digits")
    private String code;

    public TwoFaVerifyRequest(){}

    public String getTempToken(){ return tempToken;}
    public void setTempToken(String tempToken){ this.tempToken= tempToken;}

    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

}
