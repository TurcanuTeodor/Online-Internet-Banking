package ro.app.client.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.app.client.audit.AuditService;
import ro.app.client.dto.ClientDTO;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.dto.ViewClientDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.security.OwnershipChecker;
import ro.app.client.service.ClientContactService;
import ro.app.client.service.ClientProfileService;
import ro.app.client.service.ClientViewProjectionService;

@RestController
@RequestMapping("/api/clients")
@Validated
public class ClientController {

    private final ClientProfileService clientProfileService;
    private final ClientContactService clientContactService;
    private final ClientViewProjectionService clientViewProjectionService;
    private final OwnershipChecker ownershipChecker;
    private final AuditService auditService;

    public ClientController(
            ClientProfileService clientProfileService,
            ClientContactService clientContactService,
            ClientViewProjectionService clientViewProjectionService,
            OwnershipChecker ownershipChecker,
            AuditService auditService) {
        this.clientProfileService = clientProfileService;
        this.clientContactService = clientContactService;
        this.clientViewProjectionService = clientViewProjectionService;
        this.ownershipChecker = ownershipChecker;
        this.auditService = auditService;
    }

    /**
     * Public sign-up step 1: create CLIENT row (no JWT). Frontend then calls auth-service /register with returned {@code id} as {@code clientId}.
     * Uses fallback encryption key since no JWT is available yet.
     */
    @PostMapping("/sign-up")
    public ResponseEntity<ClientDTO> signUp(@Valid @RequestBody ClientDTO dto) throws Exception {
        ClientDTO created = clientProfileService.createClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 1) Create client (PF/PJ) - ADMIN only
    @PostMapping
    public ResponseEntity<ClientDTO> create(
            @Valid @RequestBody ClientDTO dto,
            @AuthenticationPrincipal JwtPrincipal principal) throws Exception {
        String ek = principal != null ? principal.encryptionKey() : null;
        ClientDTO created = clientProfileService.createClient(dto, ek);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 2) Search clients by name
    @GetMapping("/search")
    public ResponseEntity<List<ClientDTO>> search(
            @RequestParam String name,
            @AuthenticationPrincipal JwtPrincipal principal) {
        String ek = principal != null ? principal.encryptionKey() : null;
        return ResponseEntity.ok(clientProfileService.searchByName(name, ek));
    }

    // 3) Update client contact info - ownership check
    @PutMapping("/{clientId}/contact")
    public ResponseEntity<ContactInfoDTO> updateContact(
            @PathVariable Long clientId,
            @Valid @RequestBody ContactInfoDTO dto,
            @RequestHeader(value = "X-TOTP-Code", required = false) String totpCode,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, clientId);
        String ek = principal != null ? principal.encryptionKey() : null;
        ContactInfoDTO updated = clientContactService.updateClientContactInfo(clientId, dto, ek, totpCode);
        return ResponseEntity.ok(updated);
    }

    // 4) Get client summary (only client data, no accounts/transactions)
    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, id);
        String ek = principal != null ? principal.encryptionKey() : null;
        return ResponseEntity.ok(clientProfileService.getClientSummary(id, ek));
    }

    // 5) Soft delete (active=false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        clientProfileService.deleteClient(id);
        Long actorClientId = principal != null ? principal.clientId() : null;
        String role = principal != null ? principal.role() : "UNKNOWN";
        auditService.log(AuditService.CLIENT_DELETE, actorClientId, role, id, "Client soft-deleted (active=false)");
        return ResponseEntity.noContent().build();
    }

    /** Admin: suspend client (active=false). Idempotent if already inactive. */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<Void> suspend(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        clientProfileService.suspendClient(id);
        Long actorClientId = principal != null ? principal.clientId() : null;
        String role = principal != null ? principal.role() : "UNKNOWN";
        auditService.log(AuditService.ACCOUNT_FREEZE, actorClientId, role, id, "Client suspended (active=false)");
        return ResponseEntity.noContent().build();
    }

    /** USER: decrypted own row from client_readonly view (PII visible only to self). */
    @GetMapping("/view/me")
    public ResponseEntity<ViewClientDTO> viewMe(@AuthenticationPrincipal JwtPrincipal principal) {
        if (principal == null || principal.clientId() == null) {
            throw new AccessDeniedException("Client context required");
        }
        String ek = principal.encryptionKey();
        return ResponseEntity.ok(clientViewProjectionService.getViewClientForSelf(principal.clientId(), ek));
    }

    // 6) View read-only clients (ADMIN only — analytic fields only, no PII; protejat în SecurityConfig)
    @GetMapping("/view")
    public ResponseEntity<List<ViewClientDTO>> viewAll(
        @AuthenticationPrincipal JwtPrincipal principal
    ) {
        auditService.log(AuditService.ADMIN_CLIENT_LIST, principal.clientId(), principal.role(), null, "Admin accessed client analytic list");
        return ResponseEntity.ok(clientViewProjectionService.getAllViewClients());
    }

}
