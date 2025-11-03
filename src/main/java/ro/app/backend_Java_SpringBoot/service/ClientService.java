package ro.app.backend_Java_SpringBoot.service;

import org.springframework.stereotype.Service;

import ro.app.backend_Java_SpringBoot.DTO.ContactInfoDTO;
import ro.app.backend_Java_SpringBoot.DTO.mapper.ContactInfoMapper;
import ro.app.backend_Java_SpringBoot.exception.ResourceNotFoundException;
import ro.app.backend_Java_SpringBoot.model.*;
import ro.app.backend_Java_SpringBoot.repository.*;
import jakarta.transaction.Transactional;

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
    public ClientTable createClient(ClientTable client) {
        boolean exists = clientRepository
                .findByLastNameContainingIgnoreCase(client.getLastName())
                .stream()
                .anyMatch(c -> c.getFirstName().equalsIgnoreCase(client.getFirstName()));

        if (exists) {
            throw new IllegalArgumentException("Client already exists: " + client.getFirstName() + " " + client.getLastName());
        }

        return clientRepository.save(client);
    }

    // --2 Find clients by name
    public List<ClientDTO> findClientByName(String name){
        List<ClientTable> clients= clientRepository.findByLastNameContainingIgnoreCaseorFirstNameContaingIgnoreCase(name, name);
        return clients.stream()
                    .map(ClientMapper:DTO)
                    .collect(Collectors.toList()); 
    }


    // --3 Update contact info (temporar comentat)
    @Transactional
    public ContactInfo updateClientContactInfo(Long clientId, ContactInfoDTO dto) {
        ClientTable client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID " + clientId));

        ContactInfo contactInfo = contactInfoRepository.findByClientId(clientId);
        if (contactInfo == null) {
            contactInfo = ContactInfoMapper.toEntity(dto, client);
        } else {
            ContactInfoMapper.updateEntity(contactInfo, dto);
        }

        return contactInfoRepository.save(contactInfo);
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
        summary.put("client", client);
        summary.put("totalAccounts", accounts.size());
        summary.put("totalBalance", totalBalance);
        summary.put("recentTransactions", recentTransactions);
        return summary;
    }

    // --5 Soft delete
    public void deleteClient(Long id) {
        ClientTable client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (!client.isActive()) {
            throw new IllegalStateException("Client already inactive");
        }
        client.setActive(false);
        clientRepository.save(client);
    }

    // --6 View clients (read-only)
    public List<ViewClientTable> getAllViewClients() {
        return viewClientRepository.findAll();
    }
}
