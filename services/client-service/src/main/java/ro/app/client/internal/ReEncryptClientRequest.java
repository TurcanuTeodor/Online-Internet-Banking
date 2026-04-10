package ro.app.client.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReEncryptClientRequest(
        @NotNull Long clientId,
        @NotBlank String oldEncryptionKey,
        @NotBlank String newEncryptionKey
) {
}