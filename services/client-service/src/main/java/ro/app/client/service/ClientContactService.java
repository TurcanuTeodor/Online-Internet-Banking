package ro.app.client.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.dto.mapper.ContactInfoMapper;
import ro.app.client.exception.ResourceNotFoundException;
import ro.app.client.exception.StepUpRequiredException;
import ro.app.client.model.entity.Client;
import ro.app.client.model.entity.ContactInfo;
import ro.app.client.repository.ClientRepository;
import ro.app.client.repository.ContactInfoRepository;

@Service
@Validated
public class ClientContactService {

    private final ClientRepository clientRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final EncryptionService encryptionService;
    private final ClientKeyResolver keyResolver;
    private final AuthStepUpClient authStepUpClient;

    public ClientContactService(
            ClientRepository clientRepository,
            ContactInfoRepository contactInfoRepository,
            EncryptionService encryptionService,
            ClientKeyResolver keyResolver,
            AuthStepUpClient authStepUpClient) {
        this.clientRepository = clientRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.encryptionService = encryptionService;
        this.keyResolver = keyResolver;
        this.authStepUpClient = authStepUpClient;
    }

    @Transactional
    public ContactInfoDTO updateClientContactInfo(
            @NotNull Long clientId,
            ContactInfoDTO dto,
            String encryptionKey,
            String totpCode) {
        String key = keyResolver.resolveKey(encryptionKey);
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        if (totpCode == null || totpCode.isBlank()) {
            throw new StepUpRequiredException("This action requires two-factor authentication.");
        }
        authStepUpClient.verifyStepUp(clientId, totpCode);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID " + clientId));

        ContactInfo contactInfo = Optional.ofNullable(contactInfoRepository.findByClientId(clientId))
                .map(existing -> ContactInfoMapper.updateEntity(existing, dto, encryptionService, key))
                .orElseGet(() -> ContactInfoMapper.toEntity(dto, client, encryptionService, key));

        ContactInfo saved = contactInfoRepository.save(Objects.requireNonNull(contactInfo));
        return ContactInfoMapper.toDTO(saved, encryptionService, key, keyResolver.fallbackKey());
    }
}