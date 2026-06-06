package ro.app.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ro.app.transaction.exception.ResourceNotFoundException;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.model.view.ViewTransaction;
import ro.app.transaction.repository.TransactionRepository;
import ro.app.transaction.repository.ViewTransactionRepository;

/**
 * Teste unitare pentru TransactionQueryService.
 *
 * Acopera toate metodele de interogare: findBy, getById, save, anonymize.
 * Mockito este folosit strict pentru repository-uri.
 *
 * Tehnici aplicate:
 * - Boundary Value Analysis: lista nula vs. lista vida vs. lista cu elemente
 * - Decision Coverage: ramura null/empty vs. ramura cu date
 * - @Test(expected=...) pentru scenarii de exceptie
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionQueryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ViewTransactionRepository viewTransactionRepository;

    @InjectMocks
    private TransactionQueryService transactionQueryService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Transaction buildTransaction(Long id, Long accountId) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAccountId(accountId);
        t.setSign("+");
        t.setDetails("Transfer");
        t.setAmount(BigDecimal.valueOf(100));
        return t;
    }

    // ── getTransactionsByAccountId ────────────────────────────────────────────

    @Test
    public void getTransactionsByAccountId_existingAccount_returnsOrderedList() {
        // Arrange
        Transaction t1 = buildTransaction(1L, 10L);
        Transaction t2 = buildTransaction(2L, 10L);
        Mockito.when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(10L))
               .thenReturn(Arrays.asList(t1, t2));

        // Act
        List<Transaction> result = transactionQueryService.getTransactionsByAccountId(10L);

        // Assert
        Assert.assertEquals(2, result.size());
        Mockito.verify(transactionRepository).findByAccountIdOrderByTransactionDateDesc(10L);
    }

    // ── getTransactionsByAccountIds ───────────────────────────────────────────

    @Test
    public void getTransactionsByAccountIds_nullList_returnsEmptyList() {
        // Arrange — Boundary: null input
        List<Transaction> result = transactionQueryService.getTransactionsByAccountIds(null);

        // Assert — trebuie sa returneze lista vida, nu NPE
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
        Mockito.verify(transactionRepository, Mockito.never()).findByAccountIdIn(Mockito.any());
    }

    @Test
    public void getTransactionsByAccountIds_emptyList_returnsEmptyList() {
        // Arrange — Boundary: lista vida
        List<Transaction> result = transactionQueryService.getTransactionsByAccountIds(Collections.emptyList());

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
        Mockito.verify(transactionRepository, Mockito.never()).findByAccountIdIn(Mockito.any());
    }

    @Test
    public void getTransactionsByAccountIds_validList_delegatesToRepository() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Transaction t = buildTransaction(1L, 1L);
        Mockito.when(transactionRepository.findByAccountIdIn(ids)).thenReturn(Arrays.asList(t));

        // Act
        List<Transaction> result = transactionQueryService.getTransactionsByAccountIds(ids);

        // Assert
        Assert.assertEquals(1, result.size());
        Mockito.verify(transactionRepository).findByAccountIdIn(ids);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    public void getById_existingId_returnsTransaction() {
        // Arrange
        Transaction t = buildTransaction(5L, 10L);
        Mockito.when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));

        // Act
        Transaction result = transactionQueryService.getById(5L);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.valueOf(5L), result.getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getById_nonExistentId_throwsResourceNotFound() {
        // Arrange — Boundary: ID inexistent
        Mockito.when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        transactionQueryService.getById(99L);
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    public void save_validTransaction_delegatesToRepository() {
        // Arrange
        Transaction t = buildTransaction(null, 10L);
        Transaction saved = buildTransaction(7L, 10L);
        Mockito.when(transactionRepository.save(t)).thenReturn(saved);

        // Act
        Transaction result = transactionQueryService.save(t);

        // Assert
        Assert.assertEquals(Long.valueOf(7L), result.getId());
        Mockito.verify(transactionRepository).save(t);
    }

    // ── anonymizeDetailsForAccountIds ─────────────────────────────────────────

    @Test
    public void anonymizeDetailsForAccountIds_nullList_returnsZeroWithoutDbCall() {
        // Act
        int count = transactionQueryService.anonymizeDetailsForAccountIds(null, "[REMOVED]");

        // Assert — Boundary: null list => no DB call, 0 rows affected
        Assert.assertEquals(0, count);
        Mockito.verify(transactionRepository, Mockito.never())
               .anonymizeDetailsForAccountIds(Mockito.any(), Mockito.any());
    }

    @Test
    public void anonymizeDetailsForAccountIds_emptyList_returnsZeroWithoutDbCall() {
        // Act
        int count = transactionQueryService.anonymizeDetailsForAccountIds(
                Collections.emptyList(), "[REMOVED]");

        // Assert — Boundary: lista vida => fara apel DB
        Assert.assertEquals(0, count);
        Mockito.verify(transactionRepository, Mockito.never())
               .anonymizeDetailsForAccountIds(Mockito.any(), Mockito.any());
    }

    @Test
    public void anonymizeDetailsForAccountIds_validList_returnsAffectedCount() {
        // Arrange
        List<Long> accountIds = Arrays.asList(1L, 2L);
        Mockito.when(transactionRepository.anonymizeDetailsForAccountIds("[GDPR]", accountIds))
               .thenReturn(5);

        // Act
        int count = transactionQueryService.anonymizeDetailsForAccountIds(accountIds, "[GDPR]");

        // Assert
        Assert.assertEquals(5, count);
    }
}
