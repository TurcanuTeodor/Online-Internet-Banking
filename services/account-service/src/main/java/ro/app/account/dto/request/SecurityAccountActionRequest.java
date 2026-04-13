package ro.app.account.dto.request;

import jakarta.validation.constraints.NotNull;

public class SecurityAccountActionRequest {

    @NotNull
    private Long accountId;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}