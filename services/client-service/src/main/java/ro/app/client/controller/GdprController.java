package ro.app.client.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.app.client.audit.AuditService;
import ro.app.client.dto.gdpr.ClientExportDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.security.OwnershipChecker;
import ro.app.client.service.ClientGdprService;

@RestController
@RequestMapping("/api/gdpr/clients")
@Validated
public class GdprController {

    private final ClientGdprService clientGdprService;
    private final OwnershipChecker ownershipChecker;
    private final AuditService auditService;

    public GdprController(
            ClientGdprService clientGdprService,
            OwnershipChecker ownershipChecker,
            AuditService auditService) {
        this.clientGdprService = clientGdprService;
        this.ownershipChecker = ownershipChecker;
        this.auditService = auditService;
    }

    // GDPR Art. 15 — Right of access / Data portability
    // ADMIN sau proprietarul pot exporta datele personale
    @GetMapping("/{id}/export")
    public ResponseEntity<ClientExportDTO> exportData(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, id);
        ClientExportDTO body = clientGdprService.exportClientData(id);
        Long actorClientId = principal != null ? principal.clientId() : null;
        String role = principal != null ? principal.role() : "UNKNOWN";
        auditService.log("GDPR_EXPORT", actorClientId, role, id, "Personal data export (Art. 15)");
        return ResponseEntity.ok(body);
    }

    // GDPR Art. 17 — Right to erasure (ADMIN or owner)
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteClientData(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, id);
        clientGdprService.performRightToErasure(id);
        return ResponseEntity.noContent().build();
    }
}