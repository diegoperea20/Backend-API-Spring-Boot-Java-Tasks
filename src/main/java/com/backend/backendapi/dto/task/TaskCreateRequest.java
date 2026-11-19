package com.backend.backendapi.dto.task;

import jakarta.validation.constraints.NotNull;

public record TaskCreateRequest(
        @NotNull(message = "El usuario es obligatorio") String user,
        @NotNull(message = "El título es obligatorio") String title,
        String description
) {
}
