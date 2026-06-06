package ro.app.transaction.controller;

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

import ro.app.transaction.dto.TransactionDTO;
import ro.app.transaction.model.entity.Transaction;
import ro.app.transaction.security.JwtPrincipal;
import ro.app.transaction.security.OwnershipChecker;
import ro.app.transaction.service.TransactionService;

/**
 * Teste unitare pentru TransactionController.
 *
 * Pattern testat: Facade (Structural) — controllerul este punctul de intrare unic
 * care verifica ownership si delega catre TransactionService.
 *
 * Principii aplicate:
 * - AAA (Arrange–Act–Assert) in fiecare test
 * - JwtPrincipal construit cu "new" (record simplu, fara mock)
 * - Mockito strict doar pentru service si ownershipChecker
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionControllerTest {

    @Mock
    private OwnershipChecker ownershipChecker;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private JwtPrincipal clientPrincipal;

    @Before
    public void setUp() {
        clientPrincipal = new JwtPrincipal("ion.pop", 123L, "CLIENT");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Transaction buildTransaction(Long id, Long accountId) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAccountId(accountId);
        t.setSign("-");
        t.setDetails("Test transaction");
        return t;
    }

    // ── getByAccountId ────────────────────────────────────────────────────────

    @Test
    public void getByAccountId_ownershipCheckCalled_withClientId() {
        // Arrange
        Mockito.when(transactionService.getTransactionsByAccountId(10L))
               .thenReturn(Collections.emptyList());

        // Act
        transactionController.getByAccountId(10L, 123L, clientPrincipal);

        // Assert — ownership verificat cu clientId (nu accountId)
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
    }

    @Test
    public void getByAccountId_returnsListFromService() {
        // Arrange
        Transaction t1 = buildTransaction(1L, 10L);
        Transaction t2 = buildTransaction(2L, 10L);
        Mockito.when(transactionService.getTransactionsByAccountId(10L))
               .thenReturn(Arrays.asList(t1, t2));

        // Act
        List<TransactionDTO> result = transactionController.getByAccountId(10L, 123L, clientPrincipal);

        // Assert
        Assert.assertEquals(2, result.size());
    }

    // ── getByAccountIds ───────────────────────────────────────────────────────

    @Test
    public void getByAccountIds_ownershipCheckCalled() {
        // Arrange
        List<Long> accountIds = Arrays.asList(10L, 11L);
        Mockito.when(transactionService.getTransactionsByAccountIds(accountIds))
               .thenReturn(Collections.emptyList());

        // Act
        transactionController.getByAccountIds(accountIds, 123L, clientPrincipal);

        // Assert
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
    }

    @Test
    public void getByAccountIds_emptyAccountIds_returnsEmptyList() {
        // Arrange — Boundary: lista vida
        Mockito.when(transactionService.getTransactionsByAccountIds(Collections.emptyList()))
               .thenReturn(Collections.emptyList());

        // Act
        List<TransactionDTO> result = transactionController.getByAccountIds(
                Collections.emptyList(), 123L, clientPrincipal);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    // ── getByType ─────────────────────────────────────────────────────────────

    @Test
    public void getByType_validTypeCode_delegatesToService() {
        // Arrange
        Mockito.when(transactionService.getTransactionsByType("DEPOSIT"))
               .thenReturn(Collections.emptyList());

        // Act
        List<TransactionDTO> result = transactionController.getByType("DEPOSIT");

        // Assert
        Assert.assertNotNull(result);
        Mockito.verify(transactionService).getTransactionsByType("DEPOSIT");
    }

    // ── getFlagged ────────────────────────────────────────────────────────────

    @Test
    public void getFlagged_returnsListFromService() {
        // Arrange
        Transaction flagged = buildTransaction(5L, 10L);
        flagged.setFlagged(true);
        Mockito.when(transactionService.getFlaggedTransactions()).thenReturn(Arrays.asList(flagged));

        // Act
        List<TransactionDTO> result = transactionController.getFlagged();

        // Assert
        Assert.assertEquals(1, result.size());
    }

    // ── getAllFromView ─────────────────────────────────────────────────────────

    @Test
    public void getAllFromView_delegatesToService() {
        // Arrange
        Mockito.when(transactionService.getAllView()).thenReturn(Collections.emptyList());

        // Act
        transactionController.getAllFromView();

        // Assert
        Mockito.verify(transactionService).getAllView();
    }
}
