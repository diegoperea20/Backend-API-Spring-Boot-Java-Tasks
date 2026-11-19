package com.backend.backendapi.dto.auth;

import java.time.LocalDateTime;

public record HealthResponse(
        String status,
        String message,
        String database,
        LocalDateTime timestamp
) {
}
