package ro.app.banking.dto.auth;

public class TwoFaVerifyRequest {
    private String tempToken;
    private String code;

    public TwoFaVerifyRequest(){}

    public String getTempToken(){ return tempToken;}
    public void setTempToken(String tempToken){ this.tempToken= tempToken;}

    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

}
