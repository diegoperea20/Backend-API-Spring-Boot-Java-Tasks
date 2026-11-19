package com.backend.backendapi.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        String message,
        @JsonProperty("access_token") String accessToken,
        UserDto user
) {
}
