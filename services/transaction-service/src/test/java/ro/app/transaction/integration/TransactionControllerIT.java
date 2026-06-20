package ro.app.transaction.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String JWT_CLIENT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.client";
    private static final String JWT_ADMIN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin";

    @Test
    public void clientCannotAccessOtherClientTransactions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/by-account/1")
                .param("clientId", "999")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    public void adminCanAccessAnyClientTransactions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/by-account/1")
                .param("clientId", "999")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    public void clientCanAccessOwnTransactions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/by-account/1")
                .param("clientId", "123")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }

    @Test
    public void readOnlyEndpointAccessibleForAll() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/transactions/view-all")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }
}
