package com.backend.backendapi.dto.auth;

public record ProtectedResponse(String message, UserDto user) {
}
