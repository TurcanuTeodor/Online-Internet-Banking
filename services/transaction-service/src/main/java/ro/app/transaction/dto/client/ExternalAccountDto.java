package ro.app.transaction.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Subset of account-service {@code AccountDTO} JSON for inter-service REST calls.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalAccountDto {

    private Long id;
    private Long clientId;
    private String iban;
    private String currencyCode;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
