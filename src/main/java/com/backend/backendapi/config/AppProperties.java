package com.backend.backendapi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuracion tipada de la aplicacion. Se enlaza desde application.properties
 * y admite las mismas variables de entorno que usaba el backend FastAPI.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String frontendUrl,
        @NotNull @Valid Cors cors,
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid AuthCode authCode,
        @NotNull @Valid RateLimit rateLimit,
        @NotNull @Valid Google google
) {

    public record Cors(@NotEmpty List<String> origins) {
    }

    public record Jwt(@NotBlank String secret, @Min(1) long expirationMinutes) {
    }

    public record AuthCode(@Min(1) long ttlMinutes) {
    }

    public record RateLimit(
            boolean enabled,
            @Min(1) int registerPerMinute,
            @Min(1) int loginPerMinute,
            @Min(1) int exchangePerMinute,
            @Min(1) int taskWritePerMinute,
            @Min(1) int deleteAllPerMinute,
            @Min(1) int maxBuckets
    ) {
    }

    public record Google(String clientId, String clientSecret, @NotBlank String redirectUri) {

        public boolean configured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
