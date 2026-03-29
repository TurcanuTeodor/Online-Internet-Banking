package ro.app.fraud.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ro.app.fraud.internal.InternalApiHeaders;

@Component
public class TransactionRestClient {

    private static final Logger log = LoggerFactory.getLogger(TransactionRestClient.class);

    private final RestTemplate restTemplate;
    private final String transactionServiceUrl;
    private final String internalSecret;

    public TransactionRestClient(
            RestTemplate restTemplate,
            @Value("${app.services.transaction.url}") String transactionServiceUrl,
            @Value("${app.internal.api-secret}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.transactionServiceUrl = transactionServiceUrl.replaceAll("/$", "");
        this.internalSecret = internalSecret;
    }

    public List<ExternalTransactionDto> getTransactionsByAccount(Long accountId) {
        String url = transactionServiceUrl + "/api/internal/transactions/by-account/" + accountId;

        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalApiHeaders.SECRET_HEADER, internalSecret);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<List<ExternalTransactionDto>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});
            return resp.getBody() != null ? resp.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch transactions for account {}: {}", accountId, e.getMessage());
            return List.of();
        }
    }
}
