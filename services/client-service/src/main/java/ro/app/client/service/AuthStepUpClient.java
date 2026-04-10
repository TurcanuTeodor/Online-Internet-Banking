package ro.app.client.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ro.app.client.exception.BusinessRuleViolationException;
import ro.app.client.exception.StepUpRequiredException;
import ro.app.client.internal.InternalApiHeaders;

@Component
public class AuthStepUpClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String internalApiSecret;

    public AuthStepUpClient(
            RestTemplate restTemplate,
            @Value("${app.services.auth.url:http://auth-service:8081}") String authServiceUrl,
            @Value("${app.internal.api-secret}") String internalApiSecret) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl.replaceAll("/$", "");
        this.internalApiSecret = internalApiSecret;
    }

    public void verifyStepUp(Long clientId, String totpCode) {
        try {
            String url = authServiceUrl + "/api/internal/auth/step-up";
            Map<String, Object> body = new HashMap<>();
            body.put("clientId", clientId);
            body.put("totpCode", totpCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(InternalApiHeaders.SECRET, internalApiSecret);

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 428) {
                throw new StepUpRequiredException("2FA must be enabled to perform this action.");
            }
            if (code == 401) {
                throw new BusinessRuleViolationException("Invalid 2FA code.");
            }
            throw new BusinessRuleViolationException("Could not verify 2FA code.");
        } catch (RestClientException e) {
            throw new BusinessRuleViolationException("Could not verify 2FA code.");
        }
    }
}