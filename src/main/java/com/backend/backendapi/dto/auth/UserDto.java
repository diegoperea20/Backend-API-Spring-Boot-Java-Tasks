package com.backend.backendapi.dto.auth;

import com.backend.backendapi.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("last_login") LocalDateTime lastLogin
) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getCreatedAt(), user.getLastLogin());
    }
}
