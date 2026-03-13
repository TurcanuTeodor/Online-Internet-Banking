package ro.app.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import ro.app.client.dto.ClientDTO;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.dto.mapper.ClientMapper;
import ro.app.client.dto.mapper.ContactInfoMapper;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.embedded.ContactInfo;
import ro.app.client.model.entity.Client;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;
import ro.app.client.model.view.ViewClient;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;
import ro.app.client.repository.ViewClientRepository;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ViewClientRepository viewClientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;


    @Value("${encryption.key}")
    private String encryptionKey;

    public ClientService(ClientRepository clientRepository,
                         ViewClientRepository viewClientRepository,
                         ContactInfoRepository contactInfoRepository,
                         EncryptionService encryptionService) {
        this.clientRepository = clientRepository;
        this.viewClientRepository = viewClientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
    }

    // --1 Create client
    @Transactional
    public ClientDTO createClient(ClientDTO dto) throws Exception {
        boolean exists = clientRepository
                .findByLastNameContainingIgnoreCase(dto.getLastName())
                .stream()
                .anyMatch(c -> c.getFirstName().equalsIgnoreCase(dto.getFirstName()));

        if (exists) {
            throw new IllegalArgumentException(
                    "Client already exists: " + dto.getFirstName() + " " + dto.getLastName());
        }

        ClientType clientType = ClientType.fromCode(dto.getClientTypeCode());
        SexType sexType = SexType.fromCode(dto.getSexCode());

        Client entity = ClientMapper.toEntity(dto, clientType, sexType);

        entity.setFirstName(encryptionService.encrypt(entity.getFirstName(), encryptionKey));
        entity.setLastName(encryptionService.encrypt(entity.getLastName(), encryptionKey));

        final Client saved = clientRepository.save(entity);
        return ClientMapper.toDTO(saved, encryptionService, encryptionKey);
    }

    // --2 Find clients by name
    @Transactional
    public List<ClientDTO> searchByName(String name) {
        return clientRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(name, name)
                .stream()
                .map(c -> ClientMapper.toDTO(c, encryptionService, encryptionKey))
                .collect(Collectors.toList());
    }

    // --3 Update contact info
    @Transactional
    public ContactInfoDTO updateClientContactInfo(@NotNull Long clientId, ContactInfoDTO dto) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID " + clientId));

        ContactInfo contactInfo = Optional.ofNullable(contactInfoRepository.findByClientId(clientId))
                .map(existing -> ContactInfoMapper.updateEntity(existing, dto, encryptionService, encryptionKey))
                .orElseGet(() -> ContactInfoMapper.toEntity(dto, client, encryptionService, encryptionKey));

        final ContactInfo saved = contactInfoRepository.save(contactInfo);
        return ContactInfoMapper.toDTO(saved, encryptionService, encryptionKey);
    }

    // --4 Get client summary (client data only — accounts/transactions come from other services)
    public Map<String, Object> getClientSummary(@NotNull Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ContactInfoDTO contactInfo = ContactInfoMapper.toDTO(
                contactInfoRepository.findByClientId(clientId), encryptionService, encryptionKey);
        Map<String, Object> summary = new HashMap<>();
        summary.put("client", ClientMapper.toDTO(client, encryptionService, encryptionKey));
        summary.put("contactInfo", contactInfo);
        return summary;
    }

    // --5 Soft delete
    @Transactional
    public void deleteClient(@NotNull Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (!client.isActive()) {
            throw new ResourceNotFoundException("Client already inactive");
        }
        client.setActive(false);
        clientRepository.save(client);
    }

    // --6 View clients (read-only)
    public List<ViewClient> getAllViewClients() {
        return viewClientRepository.findAll();
    }
}
