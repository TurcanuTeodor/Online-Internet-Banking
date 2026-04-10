package ro.app.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import ro.app.client.dto.ClientDTO;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.dto.mapper.ClientMapper;
import ro.app.client.dto.mapper.ContactInfoMapper;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.model.entity.Client;
import ro.app.client.model.enums.ClientType;
import ro.app.client.model.enums.SexType;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

@Service
@Validated
public class ClientProfileService {

    private final ClientRepository clientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;
    private final ClientKeyResolver keyResolver;

    public ClientProfileService(
            ClientRepository clientRepository,
            ContactInfoRepository contactInfoRepository,
            EncryptionService encryptionService,
            ClientKeyResolver keyResolver) {
        this.clientRepository = clientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
        this.keyResolver = keyResolver;
    }

    @Transactional
    public ClientDTO createClient(ClientDTO dto, String encryptionKey) throws Exception {
        String key = keyResolver.resolveKey(encryptionKey);

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
        entity.setFirstName(encryptionService.encrypt(entity.getFirstName(), key));
        entity.setLastName(encryptionService.encrypt(entity.getLastName(), key));

        Client saved = clientRepository.save(entity);
        return ClientMapper.toDTO(saved, encryptionService, key, keyResolver.fallbackKey());
    }

    @Transactional
    public ClientDTO createClient(ClientDTO dto) throws Exception {
        return createClient(dto, keyResolver.fallbackKey());
    }

    @Transactional
    public List<ClientDTO> searchByName(String name, String encryptionKey) {
        String key = keyResolver.resolveKey(encryptionKey);
        return clientRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(name, name)
                .stream()
                .map(c -> ClientMapper.toDTO(c, encryptionService, key, keyResolver.fallbackKey()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getClientSummary(@NotNull Long clientId, String encryptionKey) {
        String key = keyResolver.resolveKey(encryptionKey);
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ContactInfoDTO contactInfo = ContactInfoMapper.toDTO(
                contactInfoRepository.findByClientId(clientId),
                encryptionService,
                key,
                keyResolver.fallbackKey());

        Map<String, Object> summary = new HashMap<>();
        summary.put("client", ClientMapper.toDTO(client, encryptionService, key, keyResolver.fallbackKey()));
        summary.put("contactInfo", contactInfo);
        return summary;
    }

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

    @Transactional
    public void suspendClient(@NotNull Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (client.isActive()) {
            client.setActive(false);
            clientRepository.save(client);
        }
    }
}