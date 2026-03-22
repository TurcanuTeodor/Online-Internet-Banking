package ro.app.auth.internal;

import jakarta.validation.constraints.NotNull;

public class GdprClientIdRequest {

    @NotNull
    private Long clientId;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
