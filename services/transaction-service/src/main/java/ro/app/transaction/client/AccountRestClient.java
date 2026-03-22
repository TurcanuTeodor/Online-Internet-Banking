package ro.app.transaction.client;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import ro.app.transaction.exception.ResourceNotFoundException;

/**
 * Calls account-service with the end-user JWT (forwarded from the gateway) — same pattern as payment-service {@code AccountRestClient}.
 */
@Component
public class AccountRestClient {

    private final RestTemplate restTemplate;
    private final String accountServiceBaseUrl;

    public AccountRestClient(
            RestTemplate restTemplate,
            @Value("${app.services.account.url}") String accountServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.accountServiceBaseUrl = accountServiceBaseUrl.replaceAll("/$", "");
    }

    /**
     * GET /api/accounts/by-client/{clientId}
     */
    public List<ExternalAccountDto> getAccountsByClient(Long clientId, String authorizationHeader) {
        HttpHeaders headers = buildAuthHeaders(authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = accountServiceBaseUrl + "/api/accounts/by-client/" + clientId;
        try {
            ResponseEntity<List<ExternalAccountDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ExternalAccountDto>>() {});
            List<ExternalAccountDto> body = response.getBody();
            return body != null ? body : List.of();
        } catch (HttpClientErrorException e) {
            throw mapAccountServiceException(e, "accounts for client " + clientId);
        }
    }

    /**
     * GET /api/accounts/by-iban/{iban} — account-service enforces JWT ownership; caller should also use {@code OwnershipChecker} on {@code clientId}.
     */
    public ExternalAccountDto getAccountByIban(String iban, String authorizationHeader) {
        String encodedIban = UriUtils.encodePathSegment(iban.trim(), StandardCharsets.UTF_8);
        HttpHeaders headers = buildAuthHeaders(authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = accountServiceBaseUrl + "/api/accounts/by-iban/" + encodedIban;
        try {
            ResponseEntity<ExternalAccountDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ExternalAccountDto.class);
            ExternalAccountDto account = response.getBody();
            if (account == null || account.getId() == null) {
                throw new ResourceNotFoundException("Account not found for IBAN");
            }
            return account;
        } catch (HttpClientErrorException e) {
            throw mapAccountServiceException(e, "account by IBAN");
        }
    }

    private static HttpHeaders buildAuthHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            } else {
                headers.setBearerAuth(authorizationHeader);
            }
        }
        return headers;
    }

    private RuntimeException mapAccountServiceException(HttpClientErrorException e, String context) {
        int code = e.getStatusCode().value();
        if (code == HttpStatus.NOT_FOUND.value()) {
            return new ResourceNotFoundException("Account service: not found (" + context + ")");
        }
        if (code == HttpStatus.FORBIDDEN.value()) {
            return new org.springframework.security.access.AccessDeniedException(
                    "Account service: access denied (" + context + ")");
        }
        return new IllegalStateException("Account service error (" + context + "): " + e.getMessage(), e);
    }
}
