package ro.app.client.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ro.app.client.dto.ClientExportDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.security.OwnershipChecker;
import ro.app.client.service.ClientService;

@RestController
@RequestMapping("/api/gdpr/clients")
@Validated
public class GdprController {

    private final ClientService clientService;
    private final OwnershipChecker ownershipChecker;

    public GdprController(ClientService clientService, OwnershipChecker ownershipChecker) {
        this.clientService = clientService;
        this.ownershipChecker = ownershipChecker;
    }

    // GDPR Art. 15 — Right of access / Data portability
    // ADMIN sau proprietarul pot exporta datele personale
    @GetMapping("/{id}/export")
    public ResponseEntity<ClientExportDTO> exportData(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, id);
        return ResponseEntity.ok(clientService.exportClientData(id));
    }
}