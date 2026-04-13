package ro.app.account.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import ro.app.account.audit.AuditService;
import ro.app.account.dto.request.SensitiveDataRevealAuditRequest;
import ro.app.account.dto.request.SensitiveDataRevealReasonCode;
import ro.app.account.dto.response.SensitiveDataRevealAuditEventResponse;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.service.SensitiveDataRevealAuditEventService;

@RestController
@RequestMapping("/api/accounts/audit")
public class AdminAuditController {

    private final AuditService auditService;
    private final SensitiveDataRevealAuditEventService revealAuditEventService;

    public AdminAuditController(
            AuditService auditService,
            SensitiveDataRevealAuditEventService revealAuditEventService) {
        this.auditService = auditService;
        this.revealAuditEventService = revealAuditEventService;
    }

    @PostMapping("/reveal")
    public ResponseEntity<Map<String, String>> logReveal(
            @Valid @RequestBody SensitiveDataRevealAuditRequest body,
            @AuthenticationPrincipal JwtPrincipal principal) {
        Long actorClientId = principal != null ? principal.clientId() : null;
        String role = principal != null ? principal.role() : "UNKNOWN";
        String reasonDetails = normalizeReasonDetails(body.getReasonDetails());

        if (SensitiveDataRevealReasonCode.OTHER.equals(body.getReasonCode()) && reasonDetails.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reasonDetails must be provided for OTHER and be at least 8 characters");
        }

        String details = "scope=" + body.getScope()
                + " | targetType=" + body.getTargetType()
                + " | targetId=" + body.getTargetId()
                + " | reasonCode=" + body.getReasonCode()
                + " | reasonDetails=" + reasonDetails;
        auditService.log("SENSITIVE_DATA_REVEAL", actorClientId, role, null, details);

        revealAuditEventService.record(
            principal != null ? principal.username() : "unknown",
            actorClientId,
            role,
            body.getScope(),
            body.getTargetType(),
            body.getTargetId(),
            body.getReasonCode(),
            reasonDetails);

        return ResponseEntity.ok(Map.of("status", "logged"));
    }

        @GetMapping("/reveal-events")
        public ResponseEntity<Page<SensitiveDataRevealAuditEventResponse>> listRevealEvents(
            @RequestParam(required = false) SensitiveDataRevealReasonCode reasonCode,
            Pageable pageable) {
            Pageable safePageable = pageable == null ? Pageable.unpaged() : pageable;
            return ResponseEntity.ok(revealAuditEventService.list(reasonCode, safePageable));
        }

    private String normalizeReasonDetails(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        // Avoid multiline log injection and oversized audit messages.
        String sanitized = raw.replace('\r', ' ').replace('\n', ' ').trim();
        return sanitized.length() > 240 ? sanitized.substring(0, 240) : sanitized;
    }
}