package ro.app.auth.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StepUpRequest(
        @NotNull Long clientId,
        @NotBlank String totpCode
) {
}