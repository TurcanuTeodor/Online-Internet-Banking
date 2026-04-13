package ro.app.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SensitiveDataRevealAuditRequest {

    @NotBlank
    private String scope;

    @NotBlank
    private String targetType;

    @NotBlank
    private String targetId;

    @NotNull
    private SensitiveDataRevealReasonCode reasonCode;

    @Size(max = 500)
    private String reasonDetails;

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public SensitiveDataRevealReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(SensitiveDataRevealReasonCode reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonDetails() { return reasonDetails; }
    public void setReasonDetails(String reasonDetails) { this.reasonDetails = reasonDetails; }
}