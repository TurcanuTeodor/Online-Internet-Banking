package ro.app.account.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIT {

    @Autowired
    private MockMvc mockMvc;

    // Exemplu: JWT-uri hardcoded pentru test (în practică, folosește un util pentru generare)
    private static final String JWT_CLIENT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.client";
    private static final String JWT_ADMIN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin";

    @Test
    void clientCannotAccessOtherClientData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/by-client/999")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden()); // AccessDeniedException
    }

    @Test
    void adminCanAccessAnyClientData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/by-client/999")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void clientCanAccessOwnData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/by-client/123")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }

    @Test
    void adminOnlyEndpointForbiddenForClient() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/accounts/1/close")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void readOnlyEndpointAccessibleForAll() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/1/balance")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }
}
