package ro.app.client.integration;

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
public class ClientControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String JWT_CLIENT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.client";
    private static final String JWT_ADMIN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin";

    @Test
    public void clientCannotAccessOtherClientSummary() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/clients/999/summary")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    public void adminPlaceholderJwt_IsForbiddenForSummary() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/clients/999/summary")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isForbidden());
    }

    @Test
    public void clientPlaceholderJwt_IsForbiddenForSummary() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/clients/123/summary")
                .header("Authorization", JWT_CLIENT))
                .andExpect(status().isForbidden());
    }

    @Test
    public void adminPlaceholderJwt_IsForbiddenForViewEndpoint() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/clients/view")
                .header("Authorization", JWT_ADMIN))
                .andExpect(status().isForbidden());
    }
}
