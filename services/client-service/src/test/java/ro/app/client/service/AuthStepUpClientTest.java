package ro.app.client.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ro.app.client.exception.BusinessRuleViolationException;
import ro.app.client.exception.StepUpRequiredException;

class AuthStepUpClientTest {

    private RestTemplate restTemplate;
    private AuthStepUpClient authStepUpClient;

    @BeforeEach
    void setUp() {
        restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        authStepUpClient = new AuthStepUpClient(restTemplate, "http://auth-service:8081", "internal-secret");
    }

    @Test
    void verifyStepUp_Success_DoesNotThrow() {
        assertDoesNotThrow(() -> authStepUpClient.verifyStepUp(7L, "123456"));
        verify(restTemplate).postForEntity(anyString(), any(), any());
    }

    @Test
    void verifyStepUp_When428_ThrowsStepUpRequired() {
        doThrow(HttpClientErrorException.create(HttpStatus.PRECONDITION_REQUIRED, "", HttpHeaders.EMPTY, new byte[0], null))
                .when(restTemplate)
            .postForEntity(anyString(), any(), any());

        assertThrows(StepUpRequiredException.class, () -> authStepUpClient.verifyStepUp(7L, "123456"));
    }

    @Test
    void verifyStepUp_When401_ThrowsBusinessRuleViolation() {
        doThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", HttpHeaders.EMPTY, new byte[0], null))
                .when(restTemplate)
            .postForEntity(anyString(), any(), any());

        assertThrows(BusinessRuleViolationException.class, () -> authStepUpClient.verifyStepUp(7L, "123456"));
    }
}
