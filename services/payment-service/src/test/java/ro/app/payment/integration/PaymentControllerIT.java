package ro.app.payment.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String JWT_CLIENT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.client";
    private static final String JWT_ADMIN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin";

    @Test
    void clientCannotAccessOtherClientPayments() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/by-client/999")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAnyClientPayments() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/by-client/999")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void clientCanAccessOwnPayments() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/by-client/123")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isOk());
    }

    @Test
    void readOnlyEndpointAccessibleForAll() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/payments/by-client/123")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isOk());
    }
}
