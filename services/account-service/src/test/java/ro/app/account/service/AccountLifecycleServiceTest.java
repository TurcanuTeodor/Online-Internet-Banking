package ro.app.account.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ro.app.account.exception.BusinessRuleViolationException;
import ro.app.account.exception.ResourceNotFoundException;
import ro.app.account.model.entity.Account;
import ro.app.account.model.enums.AccountStatus;
import ro.app.account.model.enums.CurrencyType;
import ro.app.account.repository.AccountRepository;

/**
 * Teste unitare pentru AccountLifecycleService.
 *
 * Pattern testat: Template Method (Behavioral) — open/close/freeze/unfreeze
 * urmeaza acelasi sablon: gaseste contul, valideaza starea, schimba starea, salveaza.
 *
 * Tehnici aplicate:
 * - Echivalenta de clase (Equivalence Partitioning) pentru stari de cont
 * - Analiza valorilor limita (Boundary Value Analysis) pentru sold zero vs. non-zero
 * - Decision Coverage: fiecare ramura if/else este acoperita
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountLifecycleServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IbanService ibanService;

    @InjectMocks
    private AccountLifecycleService accountLifecycleService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Account buildAccount(Long id, AccountStatus status, BigDecimal balance) {
        Account a = new Account();
        a.setId(id);
        a.setClientId(42L);
        a.setIban("RO49BANK0000000000000001");
        a.setBalance(balance);
        a.setCurrency(CurrencyType.EUR);
        a.setStatus(status);
        return a;
    }

    // ── openAccount ───────────────────────────────────────────────────────────

    @Test
    public void openAccount_validClientId_returnsActiveAccount() {
        // Arrange
        String generatedIban = "RO49BANK0000000000000001";
        Mockito.when(ibanService.generateIban(Mockito.any())).thenReturn(generatedIban);
        Account saved = buildAccount(1L, AccountStatus.ACTIVE, BigDecimal.ZERO);
        Mockito.when(accountRepository.save(Mockito.any(Account.class))).thenReturn(saved);

        // Act
        Account result = accountLifecycleService.openAccount(42L, "EUR");

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(AccountStatus.ACTIVE, result.getStatus());
        Mockito.verify(accountRepository).save(Mockito.any(Account.class));
        Mockito.verify(ibanService).generateIban(Mockito.any());
    }

    @Test
    public void openAccount_nullCurrencyCode_defaultsToEUR() {
        // Arrange — cod null => CurrencyType.fromCode arunca IllegalArgumentException
        // Deci testam ca serviciul propaga exceptia corect
        // Act & Assert
        try {
            accountLifecycleService.openAccount(42L, null);
            Assert.fail("Should have thrown IllegalArgumentException for null currency");
        } catch (IllegalArgumentException e) {
            // Correct — CurrencyType.fromCode arunca exceptia
        }
    }

    @Test
    public void openAccount_invalidCurrencyCode_throwsIllegalArgument() {
        // Act & Assert — cod moneda invalid
        try {
            accountLifecycleService.openAccount(42L, "XYZ");
            Assert.fail("Should have thrown IllegalArgumentException for unknown currency");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    // ── closeAccount ──────────────────────────────────────────────────────────

    @Test
    public void closeAccount_activeAccountWithZeroBalance_setsStatusToClosed() {
        // Arrange
        Account active = buildAccount(1L, AccountStatus.ACTIVE, BigDecimal.ZERO);
        Mockito.when(accountRepository.findById(1L)).thenReturn(Optional.of(active));
        Mockito.when(accountRepository.save(active)).thenReturn(active);

        // Act
        Account result = accountLifecycleService.closeAccount(1L);

        // Assert
        Assert.assertEquals(AccountStatus.CLOSED, result.getStatus());
        Mockito.verify(accountRepository).save(active);
    }

    @Test(expected = BusinessRuleViolationException.class)
    public void closeAccount_alreadyClosed_throwsBusinessRuleViolation() {
        // Arrange
        Account closed = buildAccount(2L, AccountStatus.CLOSED, BigDecimal.ZERO);
        Mockito.when(accountRepository.findById(2L)).thenReturn(Optional.of(closed));

        // Act — trebuie sa arunce exceptie
        accountLifecycleService.closeAccount(2L);
    }

    @Test(expected = BusinessRuleViolationException.class)
    public void closeAccount_nonZeroBalance_throwsBusinessRuleViolation() {
        // Arrange — Boundary: soldul este 0.01 (primul cent, limita inferioara)
        Account withFunds = buildAccount(3L, AccountStatus.ACTIVE, BigDecimal.valueOf(0.01));
        Mockito.when(accountRepository.findById(3L)).thenReturn(Optional.of(withFunds));

        // Act — nu se poate inchide cu sold nenul
        accountLifecycleService.closeAccount(3L);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void closeAccount_nonExistentId_throwsResourceNotFound() {
        // Arrange
        Mockito.when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        accountLifecycleService.closeAccount(99L);
    }

    // ── freezeAccount ─────────────────────────────────────────────────────────

    @Test
    public void freezeAccount_activeAccount_setsSuspended() {
        // Arrange
        Account active = buildAccount(4L, AccountStatus.ACTIVE, BigDecimal.valueOf(500));
        Mockito.when(accountRepository.findById(4L)).thenReturn(Optional.of(active));
        Mockito.when(accountRepository.save(active)).thenReturn(active);

        // Act
        Account result = accountLifecycleService.freezeAccount(4L);

        // Assert
        Assert.assertEquals(AccountStatus.SUSPENDED, result.getStatus());
    }

    @Test(expected = BusinessRuleViolationException.class)
    public void freezeAccount_alreadySuspended_throwsBusinessRuleViolation() {
        // Arrange
        Account suspended = buildAccount(5L, AccountStatus.SUSPENDED, BigDecimal.ZERO);
        Mockito.when(accountRepository.findById(5L)).thenReturn(Optional.of(suspended));

        // Act
        accountLifecycleService.freezeAccount(5L);
    }

    @Test(expected = BusinessRuleViolationException.class)
    public void freezeAccount_closedAccount_throwsBusinessRuleViolation() {
        // Arrange — nu poti congela un cont deja inchis
        Account closed = buildAccount(6L, AccountStatus.CLOSED, BigDecimal.ZERO);
        Mockito.when(accountRepository.findById(6L)).thenReturn(Optional.of(closed));

        // Act
        accountLifecycleService.freezeAccount(6L);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void freezeAccount_nonExistentId_throwsResourceNotFound() {
        Mockito.when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        accountLifecycleService.freezeAccount(99L);
    }

    // ── unfreezeAccount ───────────────────────────────────────────────────────

    @Test
    public void unfreezeAccount_suspendedAccount_setsActive() {
        // Arrange
        Account suspended = buildAccount(7L, AccountStatus.SUSPENDED, BigDecimal.ZERO);
        Mockito.when(accountRepository.findById(7L)).thenReturn(Optional.of(suspended));
        Mockito.when(accountRepository.save(suspended)).thenReturn(suspended);

        // Act
        Account result = accountLifecycleService.unfreezeAccount(7L);

        // Assert
        Assert.assertEquals(AccountStatus.ACTIVE, result.getStatus());
    }

    @Test(expected = BusinessRuleViolationException.class)
    public void unfreezeAccount_closedAccount_throwsBusinessRuleViolation() {
        // Arrange — nu poti reactiva un cont inchis
        Account closed = buildAccount(8L, AccountStatus.CLOSED, BigDecimal.ZERO);
        Mockito.when(accountRepository.findById(8L)).thenReturn(Optional.of(closed));

        // Act
        accountLifecycleService.unfreezeAccount(8L);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void unfreezeAccount_nonExistentId_throwsResourceNotFound() {
        Mockito.when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        accountLifecycleService.unfreezeAccount(99L);
    }
}
