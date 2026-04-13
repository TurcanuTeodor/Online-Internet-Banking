package ro.app.account.dto.response;

import java.time.LocalDateTime;

import ro.app.account.dto.request.SensitiveDataRevealReasonCode;

public class SensitiveDataRevealAuditEventResponse {
    private Long id;
    private String actorUsername;
    private Long actorClientId;
    private String actorRole;
    private String scope;
    private String targetType;
    private String targetId;
    private SensitiveDataRevealReasonCode reasonCode;
    private String reasonDetails;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public Long getActorClientId() { return actorClientId; }
    public void setActorClientId(Long actorClientId) { this.actorClientId = actorClientId; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
