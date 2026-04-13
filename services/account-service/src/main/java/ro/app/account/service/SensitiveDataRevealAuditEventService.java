package ro.app.account.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import ro.app.account.dto.request.SensitiveDataRevealReasonCode;
import ro.app.account.dto.response.SensitiveDataRevealAuditEventResponse;
import ro.app.account.model.entity.SensitiveDataRevealAuditEvent;
import ro.app.account.repository.SensitiveDataRevealAuditEventRepository;

@Service
public class SensitiveDataRevealAuditEventService {

    private final SensitiveDataRevealAuditEventRepository repository;

    public SensitiveDataRevealAuditEventService(SensitiveDataRevealAuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(
            String actorUsername,
            Long actorClientId,
            String actorRole,
            String scope,
            String targetType,
            String targetId,
            SensitiveDataRevealReasonCode reasonCode,
            String reasonDetails) {
        SensitiveDataRevealAuditEvent event = new SensitiveDataRevealAuditEvent();
        event.setActorUsername(actorUsername != null ? actorUsername : "unknown");
        event.setActorClientId(actorClientId);
        event.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
        event.setScope(scope);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setReasonCode(reasonCode);
        event.setReasonDetails(reasonDetails);
        repository.save(event);
    }

    public Page<SensitiveDataRevealAuditEventResponse> list(@Nullable SensitiveDataRevealReasonCode reasonCode, @NonNull Pageable pageable) {
        Page<SensitiveDataRevealAuditEvent> page = reasonCode == null
                ? repository.findAll(pageable)
                : repository.findByReasonCode(reasonCode, pageable);
        return page.map(this::toResponse);
    }

    private SensitiveDataRevealAuditEventResponse toResponse(SensitiveDataRevealAuditEvent event) {
        SensitiveDataRevealAuditEventResponse dto = new SensitiveDataRevealAuditEventResponse();
        dto.setId(event.getId());
        dto.setActorUsername(event.getActorUsername());
        dto.setActorClientId(event.getActorClientId());
        dto.setActorRole(event.getActorRole());
        dto.setScope(event.getScope());
        dto.setTargetType(event.getTargetType());
        dto.setTargetId(event.getTargetId());
        dto.setReasonCode(event.getReasonCode());
        dto.setReasonDetails(event.getReasonDetails());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}
