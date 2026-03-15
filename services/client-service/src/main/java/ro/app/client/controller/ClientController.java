package ro.app.client.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ro.app.client.dto.ClientDTO;
import ro.app.client.dto.ClientExportDTO;
import ro.app.client.dto.ContactInfoDTO;
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

    public ClientController(ClientService clientService, OwnershipChecker ownershipChecker) {
        this.clientService = clientService;
        this.ownershipChecker = ownershipChecker;
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

    // 6) View read-only clients (ADMIN only — protejat în SecurityConfig)
    @GetMapping("/view")
    public ResponseEntity<List<ViewClientDTO>> viewAll() {
        return ResponseEntity.ok(clientService.getAllViewClients());
    }

}
