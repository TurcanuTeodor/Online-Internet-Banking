package ro.app.account.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ro.app.account.dto.AccountDTO;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;
import ro.app.account.repository.AccountRepository;
import ro.app.account.repository.ViewAccountRepository;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;

/**
 * Teste unitare pentru AccountQueryService.
 *
 * Acopera: interogari pe repository, normalizare IBAN, verificare ownership.
 * Mockito este folosit exclusiv pentru AccountRepository si OwnershipChecker
 * (dependinte externe). JwtPrincipal se construieste cu "new" (este un record simplu).
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountQueryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ViewAccountRepository viewAccountRepository;

    @Mock
    private OwnershipChecker ownershipChecker;

    @InjectMocks
    private AccountQueryService accountQueryService;

    private JwtPrincipal clientPrincipal;

    @Before
    public void setUp() {
        clientPrincipal = new JwtPrincipal("ion.pop", 123L, "CLIENT");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Account buildAccount(Long id, Long clientId, String iban) {
        Account a = new Account();
        a.setId(id);
        a.setClientId(clientId);
        a.setIban(iban);
        a.setBalance(BigDecimal.valueOf(500));
        a.setCurrency(CurrencyType.EUR);
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }

    // ── getAccountsByClient ───────────────────────────────────────────────────

    @Test
    public void getAccountsByClient_existingClient_returnsList() {
        // Arrange
        Account a1 = buildAccount(1L, 123L, "RO49BANK0000000000000001");
        Account a2 = buildAccount(2L, 123L, "RO49BANK0000000000000002");
        Mockito.when(accountRepository.findByClientId(123L)).thenReturn(Arrays.asList(a1, a2));

        // Act
        List<Account> result = accountQueryService.getAccountsByClient(123L);

        // Assert
        Assert.assertEquals(2, result.size());
        Mockito.verify(accountRepository).findByClientId(123L);
    }

    @Test
    public void getAccountsByClient_clientWithNoAccounts_returnsEmptyList() {
        // Arrange
        Mockito.when(accountRepository.findByClientId(999L)).thenReturn(Arrays.asList());

        // Act
        List<Account> result = accountQueryService.getAccountsByClient(999L);

        // Assert — Boundary: lista vida este un rezultat valid
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    // ── getAccountDtoForPrincipal ─────────────────────────────────────────────

    @Test
    public void getAccountDtoForPrincipal_validOwner_returnsDto() {
        // Arrange
        Account a = buildAccount(1L, 123L, "RO49BANK0000000000000001");
        Mockito.when(accountRepository.findById(1L)).thenReturn(Optional.of(a));
        Mockito.doNothing().when(ownershipChecker).checkOwnership(clientPrincipal, 123L);

        // Act
        AccountDTO result = accountQueryService.getAccountDtoForPrincipal(1L, clientPrincipal);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals("RO49BANK0000000000000001", result.getIban());
        Assert.assertEquals(Long.valueOf(123L), result.getClientId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getAccountDtoForPrincipal_nonExistentId_throwsResourceNotFound() {
        // Arrange
        Mockito.when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        accountQueryService.getAccountDtoForPrincipal(99L, clientPrincipal);
    }

    // ── getAccountDtoByIban ───────────────────────────────────────────────────

    @Test
    public void getAccountDtoByIban_lowercaseIban_normalizesToUppercase() {
        // Arrange — testam normalizarea IBAN (lowercase -> uppercase)
        String lowerIban = "ro49bank0000000000000001";
        String upperIban = "RO49BANK0000000000000001";
        Account a = buildAccount(1L, 123L, upperIban);
        Mockito.when(accountRepository.findByIban(upperIban)).thenReturn(Optional.of(a));
        Mockito.doNothing().when(ownershipChecker).checkOwnership(clientPrincipal, 123L);

        // Act
        AccountDTO result = accountQueryService.getAccountDtoByIban(lowerIban, clientPrincipal);

        // Assert
        Assert.assertEquals(upperIban, result.getIban());
        Mockito.verify(accountRepository).findByIban(upperIban);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getAccountDtoByIban_nonExistentIban_throwsResourceNotFound() {
        // Arrange
        Mockito.when(accountRepository.findByIban(Mockito.anyString())).thenReturn(Optional.empty());

        // Act
        accountQueryService.getAccountDtoByIban("RO49BANK9999999999999999", clientPrincipal);
    }

    // ── getBalanceByIban ──────────────────────────────────────────────────────

    @Test
    public void getBalanceByIban_validIban_returnsBalance() {
        // Arrange
        Account a = buildAccount(1L, 123L, "RO49BANK0000000000000001");
        a.setBalance(BigDecimal.valueOf(1500.75));
        Mockito.when(accountRepository.findByIban("RO49BANK0000000000000001")).thenReturn(Optional.of(a));
        Mockito.doNothing().when(ownershipChecker).checkOwnership(clientPrincipal, 123L);

        // Act
        BigDecimal balance = accountQueryService.getBalanceByIban("RO49BANK0000000000000001", clientPrincipal);

        // Assert
        Assert.assertEquals(0, BigDecimal.valueOf(1500.75).compareTo(balance));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getBalanceByIban_nonExistentIban_throwsResourceNotFound() {
        Mockito.when(accountRepository.findByIban(Mockito.anyString())).thenReturn(Optional.empty());
        accountQueryService.getBalanceByIban("RO49BANK9999999999999999", clientPrincipal);
    }
}
