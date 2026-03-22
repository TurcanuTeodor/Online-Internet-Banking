package ro.app.client.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ro.app.client.internal.InternalApiHeaders;

/**
 * Server-to-server GDPR erasure calls (shared secret, not JWT).
 */
@Component
public class GdprDownstreamRestClient {

    private final RestTemplate restTemplate;
    private final String internalApiSecret;
    private final String authServiceBaseUrl;
    private final String accountServiceBaseUrl;
    private final String transactionServiceBaseUrl;

    public GdprDownstreamRestClient(
            RestTemplate restTemplate,
            @Value("${app.internal.api-secret}") String internalApiSecret,
            @Value("${app.services.auth.url}") String authServiceBaseUrl,
            @Value("${app.services.account.url}") String accountServiceBaseUrl,
            @Value("${app.services.transaction.url}") String transactionServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.internalApiSecret = internalApiSecret;
        this.authServiceBaseUrl = stripTrailingSlash(authServiceBaseUrl);
        this.accountServiceBaseUrl = stripTrailingSlash(accountServiceBaseUrl);
        this.transactionServiceBaseUrl = stripTrailingSlash(transactionServiceBaseUrl);
    }

    private static String stripTrailingSlash(String url) {
        return url == null ? "" : url.replaceAll("/$", "");
    }

    private HttpHeaders internalJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InternalApiHeaders.SECRET, internalApiSecret);
        return headers;
    }

    public void deactivateAuthUser(Long clientId) {
        String url = authServiceBaseUrl + "/api/internal/auth/gdpr/deactivate-user";
        HttpEntity<Map<String, Long>> entity = new HttpEntity<>(Map.of("clientId", clientId), internalJsonHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("GDPR: auth-service deactivate failed: " + e.getMessage(), e);
        }
    }

    public List<Long> fetchAccountIdsForClient(Long clientId) {
        String url = accountServiceBaseUrl + "/api/internal/accounts/gdpr/account-ids";
        HttpEntity<Map<String, Long>> entity = new HttpEntity<>(Map.of("clientId", clientId), internalJsonHeaders());
        try {
            ResponseEntity<List<Long>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<Long>>() {});
            List<Long> body = response.getBody();
            return body != null ? body : List.of();
        } catch (RestClientException e) {
            throw new IllegalStateException("GDPR: account-service account-ids failed: " + e.getMessage(), e);
        }
    }

    public void anonymizeTransactionDetails(List<Long> accountIds) {
        String url = transactionServiceBaseUrl + "/api/internal/transactions/gdpr/anonymize-details";
        Map<String, Object> body = new HashMap<>();
        body.put("accountIds", accountIds != null ? accountIds : List.of());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, internalJsonHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("GDPR: transaction-service anonymize failed: " + e.getMessage(), e);
        }
    }

    public void closeAllAccounts(Long clientId) {
        String url = accountServiceBaseUrl + "/api/internal/accounts/gdpr/close-all";
        HttpEntity<Map<String, Long>> entity = new HttpEntity<>(Map.of("clientId", clientId), internalJsonHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("GDPR: account-service close-all failed: " + e.getMessage(), e);
        }
    }
}
