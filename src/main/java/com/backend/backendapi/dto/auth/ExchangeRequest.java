package com.backend.backendapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ExchangeRequest(@NotBlank(message = "El código es obligatorio") String code) {
}
