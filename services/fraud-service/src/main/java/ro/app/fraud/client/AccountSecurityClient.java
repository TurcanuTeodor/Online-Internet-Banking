package ro.app.fraud.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ro.app.fraud.internal.InternalApiHeaders;

@Component
public class AccountSecurityClient {

    private static final Logger log = LoggerFactory.getLogger(AccountSecurityClient.class);

    private final RestTemplate restTemplate;
    private final String accountServiceUrl;
    private final String internalSecret;

    public AccountSecurityClient(
            RestTemplate restTemplate,
            @Value("${app.services.account.url:http://localhost:8083}") String accountServiceUrl,
            @Value("${app.internal.api-secret}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.accountServiceUrl = accountServiceUrl.replaceAll("/$", "");
        this.internalSecret = internalSecret;
    }

    public void freezeAccount(Long accountId) {
        postAction("freeze", accountId);
    }

    public void unfreezeAccount(Long accountId) {
        postAction("unfreeze", accountId);
    }

    private void postAction(String action, Long accountId) {
        String url = accountServiceUrl + "/api/internal/accounts/security/" + action;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InternalApiHeaders.SECRET_HEADER, internalSecret);
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(Map.of("accountId", accountId), headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to {} account {} via account-service: {}", action, accountId, e.getMessage());
            throw e;
        }
    }
}