package com.backend.backendapi.dto.auth;

import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull(message = "El username es obligatorio") String username,
        @NotNull(message = "La contraseña es obligatoria") String password
) {
}
