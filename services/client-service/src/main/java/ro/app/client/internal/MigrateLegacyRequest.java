package ro.app.client.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MigrateLegacyRequest(
        @NotNull Long clientId,
        @NotBlank String newEncryptionKey
) {
}