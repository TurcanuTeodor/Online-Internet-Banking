package ro.app.banking.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import ro.app.banking.dto.ClientDTO;
import ro.app.banking.dto.ContactInfoDTO;
import ro.app.banking.dto.mapper.ClientMapper;
import ro.app.banking.dto.mapper.ContactInfoMapper;
import ro.app.banking.exception.ResourceNotFoundException;
import ro.app.banking.model.Account;
import ro.app.banking.model.Client;
import ro.app.banking.model.ClientType;
import ro.app.banking.model.ContactInfo;
import ro.app.banking.model.SexType;
import ro.app.banking.model.Transaction;
import ro.app.banking.model.ViewClient;
import ro.app.banking.repository.AccountRepository;
import ro.app.banking.repository.ClientRepository;
import ro.app.banking.repository.ContactInfoRepository;
import ro.app.banking.repository.TransactionRepository;
import ro.app.banking.repository.ViewClientRepository;

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

        Client entity = ClientMapper.toEntity(dto, clientType, sexType);
        @SuppressWarnings("null")
        final Client saved = clientRepository.save(entity);
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
    public ContactInfoDTO updateClientContactInfo(@NotNull Long clientId, ContactInfoDTO dto) {
        if(clientId==null){
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID " + clientId));

        ContactInfo contactInfo = Optional.ofNullable(contactInfoRepository.findByClientId(clientId))
                .map(existing -> ContactInfoMapper.updateEntity(existing, dto))
                .orElseGet(() -> ContactInfoMapper.toEntity(dto, client));

        @SuppressWarnings("null")
        final ContactInfo saved = contactInfoRepository.save(contactInfo);
        return ContactInfoMapper.toDTO(saved);
    }

    // --4 Get client summary
    public Map<String, Object> getClientSummary(@NotNull Long clientId) {
        if(clientId==null){
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        final List<Account> accounts = accountRepository.findByClientId(clientId);
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> recentTransactions = transactionRepository.findByClientId(clientId)
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
    public void deleteClient(@NotNull Long id) {
        if(id==null){
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
