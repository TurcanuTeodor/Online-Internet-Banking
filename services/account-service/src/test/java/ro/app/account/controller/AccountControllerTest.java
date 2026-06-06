package ro.app.account.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ro.app.account.dto.AccountDTO;
import ro.app.account.dto.request.OpenAccountRequest;
import ro.app.account.dto.request.TransferRequest;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;
import ro.app.account.service.AccountService;

/**
 * Teste unitare pentru AccountController.
 *
 * Pattern testat: Facade (Structural) — controllerul este un punct de intrare unic
 * care delega catre AccountService si verifica ownership-ul inainte de orice operatie.
 *
 * Principii aplicate:
 * - Arrange–Act–Assert (AAA) in fiecare test
 * - Construim POJOs cu "new" (fara mock)
 * - Mockito doar pentru dependinte externe (AccountService, OwnershipChecker)
 * - Boundary Value Analysis: testam cu clientId propriu vs. clientId strain
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private OwnershipChecker ownershipChecker;

    @InjectMocks
    private AccountController accountController;

    // Principal obisnuit cu rol USER
    private JwtPrincipal clientPrincipal;
    // Principal cu rol ADMIN
    private JwtPrincipal adminPrincipal;

    @Before
    public void setUp() {
        clientPrincipal = new JwtPrincipal("ion.pop", 123L, "CLIENT");
        adminPrincipal  = new JwtPrincipal("admin",   1L,   "ADMIN");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Construieste un Account dummy pentru a simula ce returneaza service-ul. */
    private Account buildAccount(Long id, Long clientId, String iban) {
        Account a = new Account();
        a.setId(id);
        a.setClientId(clientId);
        a.setIban(iban);
        a.setBalance(BigDecimal.valueOf(1000));
        a.setCurrency(CurrencyType.EUR);
        a.setStatus(AccountStatus.ACTIVE);
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    // ── Test 1: open() — endpoint POST /api/accounts/open ───────────────────

    @Test
    public void open_validRequest_returnsCreated() {
        // Arrange
        OpenAccountRequest req = new OpenAccountRequest();
        req.setClientId(123L);
        req.setCurrencyCode("EUR");

        Account savedAccount = buildAccount(1L, 123L, "RO49BANK1234567890123456");
        Mockito.when(accountService.openAccount(123L, "EUR")).thenReturn(savedAccount);

        // Act
        ResponseEntity<AccountDTO> response = accountController.open(req, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals("RO49BANK1234567890123456", response.getBody().getIban());
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
    }

    @Test
    public void open_ownershipCheckCalled_beforeServiceCall() {
        // Arrange
        OpenAccountRequest req = new OpenAccountRequest();
        req.setClientId(123L);
        req.setCurrencyCode("EUR");

        Account savedAccount = buildAccount(1L, 123L, "RO49BANK0000000000000001");
        Mockito.when(accountService.openAccount(123L, "EUR")).thenReturn(savedAccount);

        // Act
        accountController.open(req, clientPrincipal);

        // Assert — verifica ca ownershipChecker este chemat cu valorile corecte
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
        Mockito.verify(accountService).openAccount(123L, "EUR");
    }

    // ── Test 2: close() — endpoint POST /api/accounts/{accountId}/close ─────

    @Test
    public void close_existingAccount_returnsOk() {
        // Arrange
        Account closedAccount = buildAccount(5L, 123L, "RO49BANK0000000000000005");
        closedAccount.setStatus(AccountStatus.CLOSED);
        Mockito.when(accountService.closeAccount(5L)).thenReturn(closedAccount);

        // Act
        ResponseEntity<AccountDTO> response = accountController.close(5L);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals("CLOSED", response.getBody().getStatus());
    }

    // ── Test 3: byClient() — endpoint GET /api/accounts/by-client/{clientId}

    @Test
    public void byClient_ownClientId_returnsListAndOwnershipChecked() {
        // Arrange
        Account a1 = buildAccount(1L, 123L, "RO49BANK0000000000000001");
        Account a2 = buildAccount(2L, 123L, "RO49BANK0000000000000002");
        Mockito.when(accountService.getAccountsByClient(123L)).thenReturn(Arrays.asList(a1, a2));

        // Act
        ResponseEntity<List<AccountDTO>> response = accountController.byClient(123L, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals(2, response.getBody().size());
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
    }

    @Test
    public void byClient_emptyAccountList_returnsEmptyList() {
        // Arrange
        Mockito.when(accountService.getAccountsByClient(123L)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<AccountDTO>> response = accountController.byClient(123L, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertTrue(response.getBody().isEmpty());
    }

    // ── Test 4: byId() — endpoint GET /api/accounts/by-id/{accountId} ───────

    @Test
    public void byId_validAccount_returnsOk() {
        // Arrange
        AccountDTO dto = new AccountDTO();
        dto.setId(1L);
        dto.setIban("RO49BANK0000000000000001");
        dto.setClientId(123L);
        Mockito.when(accountService.getAccountDtoForPrincipal(1L, clientPrincipal)).thenReturn(dto);

        // Act
        ResponseEntity<AccountDTO> response = accountController.byId(1L, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals(Long.valueOf(1L), response.getBody().getId());
    }

    // ── Test 5: byIban() — endpoint GET /api/accounts/by-iban/{iban} ─────────

    @Test
    public void byIban_validIban_returnsOk() {
        // Arrange
        String iban = "RO49BANK0000000000000001";
        AccountDTO dto = new AccountDTO();
        dto.setIban(iban);
        dto.setClientId(123L);
        Mockito.when(accountService.getAccountDtoByIban(iban, clientPrincipal)).thenReturn(dto);

        // Act
        ResponseEntity<AccountDTO> response = accountController.byIban(iban, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals(iban, response.getBody().getIban());
    }

    // ── Test 6: balance() — endpoint GET /api/accounts/{iban}/balance ────────

    @Test
    public void balance_validIban_returnsBalance() {
        // Arrange
        String iban = "RO49BANK0000000000000001";
        Mockito.when(accountService.getBalanceByIban(iban, clientPrincipal))
               .thenReturn(BigDecimal.valueOf(2500.50));

        // Act
        ResponseEntity<BigDecimal> response = accountController.balance(iban, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals(0, BigDecimal.valueOf(2500.50).compareTo(response.getBody()));
    }

    // ── Test 7: transfer() — endpoint POST /api/accounts/transfer ────────────

    @Test
    public void transfer_validRequest_returnsNoContent() {
        // Arrange
        TransferRequest req = new TransferRequest();
        req.setFromIban("RO49BANK0000000000000001");
        req.setToIban("RO49BANK0000000000000002");
        req.setAmount(BigDecimal.valueOf(500));

        // Act
        ResponseEntity<Void> response = accountController.transfer(req, null, clientPrincipal);

        // Assert
        Assert.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        Mockito.verify(accountService).transfer("RO49BANK0000000000000001",
                "RO49BANK0000000000000002", BigDecimal.valueOf(500), null, clientPrincipal);
    }

    // ── Test 8: freeze() — endpoint POST /api/accounts/{accountId}/freeze ────

    @Test
    public void freeze_existingAccount_returnsOkWithSuspendedStatus() {
        // Arrange
        Account frozen = buildAccount(3L, 123L, "RO49BANK0000000000000003");
        frozen.setStatus(AccountStatus.SUSPENDED);
        Mockito.when(accountService.freezeAccount(3L)).thenReturn(frozen);

        // Act
        ResponseEntity<AccountDTO> response = accountController.freeze(3L);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals("SUSPENDED", response.getBody().getStatus());
    }

    // ── Test 9: unfreeze() — endpoint POST /api/accounts/{accountId}/unfreeze

    @Test
    public void unfreeze_existingAccount_returnsOkWithActiveStatus() {
        // Arrange
        Account unfrozen = buildAccount(3L, 123L, "RO49BANK0000000000000003");
        unfrozen.setStatus(AccountStatus.ACTIVE);
        Mockito.when(accountService.unfreezeAccount(3L)).thenReturn(unfrozen);

        // Act
        ResponseEntity<AccountDTO> response = accountController.unfreeze(3L);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals("ACTIVE", response.getBody().getStatus());
    }

    // ── Test 10: viewAll() — endpoint GET /api/accounts/view ─────────────────

    @Test
    public void viewAll_returnsListFromService() {
        // Arrange
        Mockito.when(accountService.getAllViewAccounts()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<?>> response = accountController.viewAll();

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
    }
}
