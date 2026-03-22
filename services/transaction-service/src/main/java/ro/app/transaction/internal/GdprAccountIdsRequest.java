package ro.app.transaction.internal;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class GdprAccountIdsRequest {

    @NotNull
    private List<Long> accountIds;

    public List<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }
}
