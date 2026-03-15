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
import ro.app.client.dto.ClientExportDTO;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.dto.ViewClientDTO;
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

    // --6 View clients (decripteaza inainte de a returna)
    public List<ViewClientDTO> getAllViewClients() {
        return viewClientRepository.findAll()
                .stream()
                .map(v -> {
                    ViewClientDTO dto = new ViewClientDTO();
                    dto.setClientId(v.getClientId());
                    dto.setFirstName(v.getClientFirstName());
                    dto.setLastName(v.getClientLastName());
                    dto.setClientType(v.getClientTypeName());
                    dto.setActive(v.getActive());
                    dto.setCreatedAt(v.getCreatedAt());
                    try {
                        dto.setEmail(encryptionService.decrypt(v.getEmailEncrypted(), encryptionKey));
                        dto.setPhone(encryptionService.decrypt(v.getPhoneEncrypted(), encryptionKey));
                        dto.setAddress(encryptionService.decrypt(v.getAddressEncrypted(), encryptionKey));
                        dto.setCity(encryptionService.decrypt(v.getCityEncrypted(), encryptionKey));
                        dto.setPostalCode(encryptionService.decrypt(v.getPostalCodeEncrypted(), encryptionKey));
                    } catch (Exception e) {
                        // date necriptate (seed dev) — returnează ca atare
                        dto.setEmail(v.getEmailEncrypted());
                        dto.setPhone(v.getPhoneEncrypted());
                        dto.setAddress(v.getAddressEncrypted());
                        dto.setCity(v.getCityEncrypted());
                        dto.setPostalCode(v.getPostalCodeEncrypted());
                    }
                    return dto;
                })
                .toList();
    }

    //--7 GDPR Data Export
    public ClientExportDTO exportClientData(@NotNull Long clientId){
        if(clientId == null){
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ContactInfo contactInfo = contactInfoRepository.findByClientId(clientId);

        ClientExportDTO export= new ClientExportDTO();

        // date client decriptate
        export.setClientId(client.getId());
        try{
            export.setFirstName(encryptionService.decrypt(client.getFirstName(), encryptionKey));
            export.setLastName(encryptionService.decrypt(client.getLastName(), encryptionKey));
        }catch(Exception e){
            export.setFirstName(client.getFirstName());
            export.setLastName(client.getLastName());
        }
        export.setClientType(client.getClientType() != null ? client.getClientType().getCode() : null);
        export.setSexType(client.getSexType() != null ? client.getSexType().getCode() : null);
        export.setRiskLevel(client.getRiskLevel());
        export.setActive(client.isActive());

        // date contact info decriptate
        if (contactInfo != null) {
            try {
                export.setEmail(encryptionService.decrypt(contactInfo.getEmail(), encryptionKey));
                export.setPhone(encryptionService.decrypt(contactInfo.getPhone(), encryptionKey));
                export.setContactPerson(encryptionService.decrypt(contactInfo.getContactPerson(), encryptionKey));
                export.setWebsite(encryptionService.decrypt(contactInfo.getWebsite(), encryptionKey));
                export.setAddress(encryptionService.decrypt(contactInfo.getAddress(), encryptionKey));
                export.setCity(encryptionService.decrypt(contactInfo.getCity(), encryptionKey));
                export.setPostalCode(encryptionService.decrypt(contactInfo.getPostalCode(), encryptionKey));
            } catch (Exception e) {
                // fallback pentru date necriptate (seed dev)
                export.setEmail(contactInfo.getEmail());
                export.setPhone(contactInfo.getPhone());
                export.setContactPerson(contactInfo.getContactPerson());
                export.setWebsite(contactInfo.getWebsite());
                export.setAddress(contactInfo.getAddress());
                export.setCity(contactInfo.getCity());
                export.setPostalCode(contactInfo.getPostalCode());
            }
        }

        return export;
    }
}