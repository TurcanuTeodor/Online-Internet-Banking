package ro.app.banking.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.app.banking.dto.ClientDTO;
import ro.app.banking.dto.ContactInfoDTO;
import ro.app.banking.service.ClientService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // 1) Create client (PF/PJ)
    @PostMapping
    public ResponseEntity<ClientDTO> create(@Valid @RequestBody ClientDTO dto) {
        ClientDTO created = clientService.createClient(dto);  // Return DTO directly
        return ResponseEntity.ok(created);
    }

    // 2) Search clients by name
    @GetMapping("/search")
    public ResponseEntity<List<ClientDTO>> search(@RequestParam String name) {
        return ResponseEntity.ok(clientService.searchByName(name));
    }

    // 3) Update client contact info
    @PutMapping("/{clientId}/contact")
    public ResponseEntity<ContactInfoDTO> updateContact(
            @PathVariable Long clientId,
            @Valid @RequestBody ContactInfoDTO dto) {
        ContactInfoDTO updated = clientService.updateClientContactInfo(clientId, dto);
        return ResponseEntity.ok(updated);
    }

    // 4) Get client summary
    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientSummary(id));
    }

    // 5) Soft delete (active=false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    // 6) View read-only clients
    @GetMapping("/view")
    public ResponseEntity<List<?>> viewAll() {
        return ResponseEntity.ok(clientService.getAllViewClients());
    }
}
