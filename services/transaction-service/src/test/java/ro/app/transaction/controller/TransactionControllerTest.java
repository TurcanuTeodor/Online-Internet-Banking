package ro.app.transaction.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ro.app.transaction.security.JwtPrincipal;
import ro.app.transaction.security.OwnershipChecker;
import ro.app.transaction.service.TransactionService;

import java.util.List;

import static org.mockito.Mockito.*;

public class TransactionControllerTest {

    @Mock
    private OwnershipChecker ownershipChecker;
    @Mock
    private TransactionService transactionService;
    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionController = new TransactionController(transactionService, ownershipChecker);
    }

    @Test
    public void getByAccountId_OwnershipCheckCalled() {
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(transactionService.getTransactionsByAccountId(anyLong())).thenReturn(List.of());
        transactionController.getByAccountId(1L, 123L, principal);
        verify(ownershipChecker).checkOwnership(principal, 123L);
    }

    // Add similar tests for other endpoints with ownership check
}
