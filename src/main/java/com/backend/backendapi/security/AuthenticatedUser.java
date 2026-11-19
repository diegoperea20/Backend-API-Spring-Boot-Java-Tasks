package com.backend.backendapi.security;

import com.backend.backendapi.domain.User;
import com.backend.backendapi.dto.auth.UserDto;

import java.time.LocalDateTime;

/**
 * Principal que se guarda en el SecurityContext. Es un record inmutable para no
 * exponer entidades JPA fuera de la capa de persistencia.
 */
public record AuthenticatedUser(Long id, String username, LocalDateTime createdAt, LocalDateTime lastLogin) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getCreatedAt(), user.getLastLogin());
    }

    public UserDto toDto() {
        return new UserDto(id, username, createdAt, lastLogin);
    }
}
