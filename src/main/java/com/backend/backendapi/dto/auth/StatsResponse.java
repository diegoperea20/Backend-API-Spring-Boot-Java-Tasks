package com.backend.backendapi.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record StatsResponse(
        @JsonProperty("total_users") long totalUsers,
        @JsonProperty("active_auth_codes") long activeAuthCodes,
        LocalDateTime timestamp
) {
}
