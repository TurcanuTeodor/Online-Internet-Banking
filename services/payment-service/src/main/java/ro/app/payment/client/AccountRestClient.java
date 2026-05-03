package ro.app.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ro.app.payment.dto.ExternalAccountDto;

/**
 * Loads account metadata from account-service using the end-user's JWT (forwarded from the gateway).
 * 
 * Migrated from RestTemplate to RestClient (Spring Boot 3.2+) for modern, fluent HTTP API
 * and automatic observation/tracing support via Micrometer.
 */
@Component
public class AccountRestClient {

    private final RestClient restClient;
    private final String accountServiceBaseUrl;

    public AccountRestClient(
            RestClient restClient,
            @Value("${app.services.account.url}") String accountServiceBaseUrl) {
        this.restClient = restClient;
        this.accountServiceBaseUrl = accountServiceBaseUrl.replaceAll("/$", "");
    }

    public ExternalAccountDto getAccountById(Long accountId, String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return restClient.get()
                    .uri(accountServiceBaseUrl + "/api/accounts/by-id/" + accountId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ExternalAccountDto.class);
        }

        String authHeader = authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorizationHeader
                : "Bearer " + authorizationHeader;

        return restClient.get()
                .uri(accountServiceBaseUrl + "/api/accounts/by-id/" + accountId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader)
                .retrieve()
                .body(ExternalAccountDto.class);
    }
}
