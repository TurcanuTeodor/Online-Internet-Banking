package ro.app.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ro.app.payment.dto.ExternalAccountDto;

/**
 * Loads account metadata from account-service using the end-user's JWT (forwarded from the gateway).
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

    public ExternalAccountDto getAccountById(Long accountId, String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                headers.setBearerAuth(authorizationHeader);
            } else {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
                accountServiceBaseUrl + "/api/accounts/by-id/" + accountId,
                HttpMethod.GET,
                entity,
                ExternalAccountDto.class
        ).getBody();
    }
}
