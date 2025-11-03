package ro.app.backend_Java_SpringBoot.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.app.backend_Java_SpringBoot.DTO.ClientDTO;
import ro.app.backend_Java_SpringBoot.DTO.ContactInfoDTO;
import ro.app.backend_Java_SpringBoot.DTO.mapper.ClientMapper;
import ro.app.backend_Java_SpringBoot.DTO.mapper.ContactInfoMapper;
import ro.app.backend_Java_SpringBoot.model.*;
import ro.app.backend_Java_SpringBoot.service.ClientService;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    @PersistenceContext
    private EntityManager em;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // 1) Create client (PF/PJ)
    @PostMapping
    public ResponseEntity<ClientDTO> create(@Valid @RequestBody ClientDTO dto) {
        ClientType ct = em.getReference(ClientType.class, dto.getClientTypeId());
        SexType st = em.getReference(SexType.class, dto.getSexId());
        ClientTable entity = ClientMapper.toEntity(dto, ct, st);
        ClientTable saved = clientService.createClient(entity);
        return ResponseEntity.ok(ClientMapper.toDTO(saved));
    }

    // 2) Caută clienți după nume (lastName conține)
    @GetMapping("/search")
    public ResponseEntity<List<ClientDTO>> search(@RequestParam String name) {
        var clients = clientService.searchByName(name);
        var dtos = clients.stream().map(ClientMapper.INSTANCE::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    // 3) Update client contact info
    @PutMapping("/{clientId}/contact")
    public ResponseEntity<ContactInfoDTO> updateContact(
            @PathVariable Long clientId,
            @Valid @RequestBody ContactInfoDTO dto) {

        ContactInfo updated = clientService.updateClientContactInfo(clientId, dto);
        return ResponseEntity.ok(ContactInfoMapper.toDTO(updated));
    }

    // 4) Sum-up client (conturi, sold total, tranzacții recente)
    @GetMapping("/{id}/summary")
    public Map<String, Object> getSummary(@PathVariable Long id) {
        return clientService.getClientSummary(id);
    }

    // 5) Soft delete (active=false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    // View read-only din view_client
    @GetMapping("/view")
    public List<ViewClientTable> viewAll() {
        return clientService.getAllViewClients();
    }
}
