package ro.app.transaction.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
// Removed unused import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String JWT_CLIENT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.client";
    private static final String JWT_ADMIN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin";

    @Test
    void clientCannotAccessOtherClientTransactions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/by-account/1")
                .param("clientId", "999")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAnyClientTransactions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/by-account/1")
                .param("clientId", "999")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void clientCanAccessOwnTransactions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/by-account/1")
                .param("clientId", "123")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }

    @Test
    void readOnlyEndpointAccessibleForAll() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/view-all")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }
}
