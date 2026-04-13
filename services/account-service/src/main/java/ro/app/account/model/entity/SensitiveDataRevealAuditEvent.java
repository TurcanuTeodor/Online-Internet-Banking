package ro.app.account.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ro.app.account.dto.request.SensitiveDataRevealReasonCode;

@Entity
@Table(name = "\"SENSITIVE_DATA_REVEAL_AUDIT\"")
public class SensitiveDataRevealAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Column(name = "actor_client_id")
    private Long actorClientId;

    @Column(name = "actor_role", nullable = false)
    private String actorRole;

    @Column(name = "scope", nullable = false)
    private String scope;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false)
    private SensitiveDataRevealReasonCode reasonCode;

    @Column(name = "reason_details")
    private String reasonDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
