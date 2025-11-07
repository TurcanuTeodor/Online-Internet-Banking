package ro.app.backend_Java_SpringBoot.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ro.app.backend_Java_SpringBoot.dto.ClientDTO;
import ro.app.backend_Java_SpringBoot.dto.ContactInfoDTO;
import ro.app.backend_Java_SpringBoot.dto.mapper.ClientMapper;
import ro.app.backend_Java_SpringBoot.dto.mapper.ContactInfoMapper;
import ro.app.backend_Java_SpringBoot.exception.ResourceNotFoundException;
import ro.app.backend_Java_SpringBoot.model.*;
import ro.app.backend_Java_SpringBoot.repository.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ViewClientRepository viewClientRepository;
    private final ContactInfoRepository contactInfoRepository;

    public ClientService(ClientRepository clientRepository,
                         AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         ViewClientRepository viewClientRepository,
                         ContactInfoRepository contactInfoRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.viewClientRepository = viewClientRepository;
        this.contactInfoRepository = contactInfoRepository;
    }

    // --1 Create client
    @Transactional
    public ClientDTO createClient(ClientDTO dto) {
        boolean exists = clientRepository
                .findByLastNameContainingIgnoreCase(dto.getLastName())
                .stream()
                .anyMatch(c -> c.getFirstName().equalsIgnoreCase(dto.getFirstName()));

        if (exists) {
            throw new ResourceNotFoundException(
                    "Client already exists: " + dto.getFirstName() + " " + dto.getLastName());
        }

        ClientType clientType = new ClientType();
        clientType.setId(dto.getClientTypeId());

        SexType sexType = new SexType();
        sexType.setId(dto.getSexId());

        ClientTable entity = ClientMapper.toEntity(dto, clientType, sexType);
        ClientTable saved = clientRepository.save(entity);
        return ClientMapper.toDTO(saved);
    }

    // --2 Find clients by name
    public List<ClientDTO> searchByName(String name) {
        return clientRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(name, name)
                .stream()
                .map(ClientMapper::toDTO)
                .collect(Collectors.toList());
    }

    // --3 Update contact info
    @Transactional
    public ContactInfoDTO updateClientContactInfo(Long clientId, ContactInfoDTO dto) {
        ClientTable client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID " + clientId));

        ContactInfo contactInfo = Optional.ofNullable(contactInfoRepository.findByClientId(clientId))
                .map(existing -> ContactInfoMapper.updateEntity(existing, dto))
                .orElse(ContactInfoMapper.toEntity(dto, client));

        ContactInfo saved = contactInfoRepository.save(contactInfo);
        return ContactInfoMapper.toDTO(saved);
    }

    // --4 Get client summary
    public Map<String, Object> getClientSummary(Long clientId) {
        ClientTable client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<AccountTable> accounts = accountRepository.findByClientId(clientId);
        BigDecimal totalBalance = accounts.stream()
                .map(AccountTable::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionTable> recentTransactions = transactionRepository.findByClientId(clientId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("client", ClientMapper.toDTO(client));
        summary.put("totalAccounts", accounts.size());
        summary.put("totalBalance", totalBalance);
        summary.put("recentTransactions", recentTransactions);
        return summary;
    }

    // --5 Soft delete
    @Transactional
    public void deleteClient(Long id) {
        ClientTable client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (!client.isActive()) {
            throw new ResourceNotFoundException("Client already inactive");
        }
        client.setActive(false);
        clientRepository.save(client);
    }

    // --6 View clients (read-only)
    public List<ViewClientTable> getAllViewClients() {
        return viewClientRepository.findAll();
    }
}
