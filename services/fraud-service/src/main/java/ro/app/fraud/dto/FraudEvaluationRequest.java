package ro.app.fraud.dto;

import jakarta.validation.constraints.NotNull;

public class FraudEvaluationRequest {

    @NotNull
    private Long accountId;

    @NotNull
    private Long clientId;

    private Long transactionId;
    private String correlationId;

    @NotNull
    private Double amount;

    private String currency;
    private String senderIban;
    private String receiverIban;
    private String transactionType;
    private boolean selfTransfer;
    private int accountAgeDays;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSenderIban() { return senderIban; }
    public void setSenderIban(String senderIban) { this.senderIban = senderIban; }

    public String getReceiverIban() { return receiverIban; }
    public void setReceiverIban(String receiverIban) { this.receiverIban = receiverIban; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public boolean isSelfTransfer() { return selfTransfer; }
    public void setSelfTransfer(boolean selfTransfer) { this.selfTransfer = selfTransfer; }

    public int getAccountAgeDays() { return accountAgeDays; }
    public void setAccountAgeDays(int accountAgeDays) { this.accountAgeDays = accountAgeDays; }
}
