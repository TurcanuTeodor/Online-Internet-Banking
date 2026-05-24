package ro.app.account.integration;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ro.app.account.controller.AccountController;
import ro.app.account.security.JwtPrincipal;
import ro.app.account.security.OwnershipChecker;
import ro.app.account.service.AccountService;

@WebMvcTest(AccountController.class)
class AccountControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private OwnershipChecker ownershipChecker;

    @MockBean
    private ro.app.account.security.jwt.JwtService jwtService;

    @Test
    void clientCannotAccessOtherClientData() throws Exception {
        JwtPrincipal principal = Mockito.mock(JwtPrincipal.class);
        Mockito.doThrow(new AccessDeniedException("forbidden")).when(ownershipChecker).checkOwnership(any(), eq(999L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/by-client/999")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAnyClientData() throws Exception {
        JwtPrincipal principal = Mockito.mock(JwtPrincipal.class);
        Mockito.doNothing().when(ownershipChecker).checkOwnership(any(), any());
        Mockito.when(accountService.getAccountsByClient(any())).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/by-client/999")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isOk());
    }

    @Test
    void clientCanAccessOwnData() throws Exception {
        JwtPrincipal principal = Mockito.mock(JwtPrincipal.class);
        Mockito.doNothing().when(ownershipChecker).checkOwnership(any(), any());
        Mockito.when(accountService.getAccountsByClient(eq(123L))).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/by-client/123")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))))))
                .andExpect(status().isOk());
    }

    @Test
    void adminOnlyEndpointForbiddenForClient() throws Exception {
        JwtPrincipal principal = Mockito.mock(JwtPrincipal.class);
        Mockito.doNothing().when(ownershipChecker).checkOwnership(any(), any());
        Mockito.when(accountService.closeAccount(any())).thenThrow(new AccessDeniedException("forbidden"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/accounts/1/close").contentType(MediaType.APPLICATION_JSON)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void readOnlyEndpointAccessibleForAll() throws Exception {
        JwtPrincipal principal = Mockito.mock(JwtPrincipal.class);
        Mockito.when(accountService.getBalanceByIban(any(), any())).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/RO12ABC/balance")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))))))
                .andExpect(status().isOk());
    }
}
