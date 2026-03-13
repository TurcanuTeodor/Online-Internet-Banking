package ro.app.account.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.app.account.dto.AccountDTO;
import ro.app.account.dto.request.OpenAccountRequest;
import ro.app.account.model.entity.Account;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;
import ro.app.account.service.AccountService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @Mock
    private AccountService accountService;
    @Mock
    private OwnershipChecker ownershipChecker;
    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountController = new AccountController(accountService, ownershipChecker);
    }

    @Test
    void openAccount_OwnershipCheckCalled() {
        OpenAccountRequest req = mock(OpenAccountRequest.class);
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(req.getClientId()).thenReturn(123L);
        Account account = mock(Account.class);
        when(accountService.openAccount(123L, null)).thenReturn(account);

        ResponseEntity<AccountDTO> response = accountController.open(req, principal);

        verify(ownershipChecker).checkOwnership(principal, 123L);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // Adaugă teste similare pentru celelalte endpointuri cu ownership check
}
