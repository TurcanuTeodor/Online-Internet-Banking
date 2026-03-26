package ro.app.client.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ro.app.client.dto.ClientDTO;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.audit.AuditService;
import ro.app.client.dto.ViewClientDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.service.ClientService;
import ro.app.client.security.OwnershipChecker;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@Validated
public class ClientController {

    private final ClientService clientService;
    private final OwnershipChecker ownershipChecker;
    private final AuditService auditService;

    public ClientController(
            ClientService clientService,
            OwnershipChecker ownershipChecker,
            AuditService auditService) {
        this.clientService = clientService;
        this.ownershipChecker = ownershipChecker;
        this.auditService = auditService;
    }

    /**
     * Public sign-up step 1: create CLIENT row (no JWT). Frontend then calls auth-service /register with returned {@code id} as {@code clientId}.
     */
    @PostMapping("/sign-up")
    public ResponseEntity<ClientDTO> signUp(@Valid @RequestBody ClientDTO dto) throws Exception {
        ClientDTO created = clientService.createClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 1) Create client (PF/PJ) - ADMIN only
    @PostMapping
    public ResponseEntity<ClientDTO> create(@Valid @RequestBody ClientDTO dto) throws Exception {
        ClientDTO created = clientService.createClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 2) Search clients by name 
    @GetMapping("/search")
    public ResponseEntity<List<ClientDTO>> search(@RequestParam String name) {
        return ResponseEntity.ok(clientService.searchByName(name));
    }

    // 3) Update client contact info - ownership check
    @PutMapping("/{clientId}/contact")
    public ResponseEntity<ContactInfoDTO> updateContact(
            @PathVariable Long clientId,
            @Valid @RequestBody ContactInfoDTO dto,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, clientId);
        ContactInfoDTO updated = clientService.updateClientContactInfo(clientId, dto);
        return ResponseEntity.ok(updated);
    }

    // 4) Get client summary (only client data, no accounts/transactions)
    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ownershipChecker.checkOwnership(principal, id);
        return ResponseEntity.ok(clientService.getClientSummary(id));
    }

    // 5) Soft delete (active=false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    /** Admin: suspend client (active=false). Idempotent if already inactive. */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<Void> suspend(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        clientService.suspendClient(id);
        Long actorClientId = principal != null ? principal.clientId() : null;
        String role = principal != null ? principal.role() : "UNKNOWN";
        auditService.log("ACCOUNT_FREEZE", actorClientId, role, id, "Client suspended (active=false)");
        return ResponseEntity.noContent().build();
    }

    /** USER: decrypted own row from client_readonly view (PII visible only to self). */
    @GetMapping("/view/me")
    public ResponseEntity<ViewClientDTO> viewMe(@AuthenticationPrincipal JwtPrincipal principal) {
        if (principal == null || principal.clientId() == null) {
            throw new AccessDeniedException("Client context required");
        }
        return ResponseEntity.ok(clientService.getViewClientForSelf(principal.clientId()));
    }

    // 6) View read-only clients (ADMIN only — masked PII; protejat în SecurityConfig)
    @GetMapping("/view")
    public ResponseEntity<List<ViewClientDTO>> viewAll() {
        return ResponseEntity.ok(clientService.getAllViewClients());
    }

}
