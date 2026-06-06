package ro.app.client.service;

import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ro.app.client.exception.BusinessRuleViolationException;
import ro.app.client.exception.StepUpRequiredException;

/**
 * Teste unitare pentru AuthStepUpClient (JUnit 4).
 *
 * Acopera cele 3 scenarii de raspuns HTTP:
 *   1. Succes (2xx) — nu arunca exceptie
 *   2. 428 Precondition Required — arunca StepUpRequiredException
 *   3. 401 Unauthorized — arunca BusinessRuleViolationException
 *
 * Pattern testat: Strategy (Behavioral) — alegerea exceptiei in functie de codul HTTP.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthStepUpClientTest {

    private RestTemplate restTemplate;
    private AuthStepUpClient authStepUpClient;

    @Before
    public void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        authStepUpClient = new AuthStepUpClient(
                restTemplate, "http://auth-service:8081", "internal-secret");
    }

    @Test
    public void verifyStepUp_success_doesNotThrow() {
        // Arrange — RestTemplate nu arunca nimic (raspuns 2xx)
        // Mockito returneaza null implicit pentru postForEntity

        // Act & Assert — nu trebuie sa apara nicio exceptie
        authStepUpClient.verifyStepUp(7L, "123456");
        Mockito.verify(restTemplate).postForEntity(
                Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test(expected = StepUpRequiredException.class)
    public void verifyStepUp_when428_throwsStepUpRequired() {
        // Arrange — simulam raspuns HTTP 428 de la auth-service
        Mockito.doThrow(HttpClientErrorException.create(
                HttpStatus.PRECONDITION_REQUIRED, "", HttpHeaders.EMPTY, new byte[0], null))
               .when(restTemplate).postForEntity(Mockito.anyString(), Mockito.any(), Mockito.any());

        // Act
        authStepUpClient.verifyStepUp(7L, "123456");
    }

    @Test(expected = BusinessRuleViolationException.class)
    public void verifyStepUp_when401_throwsBusinessRuleViolation() {
        // Arrange — simulam raspuns HTTP 401 (cod TOTP invalid)
        Mockito.doThrow(HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "", HttpHeaders.EMPTY, new byte[0], null))
               .when(restTemplate).postForEntity(Mockito.anyString(), Mockito.any(), Mockito.any());

        // Act
        authStepUpClient.verifyStepUp(7L, "wrongCode");
    }
}
