package com.backend.backendapi.dto.task;

/**
 * En FastAPI todos los campos eran opcionales en la actualizacion: user y title
 * solo se aplicaban si eran "truthy" y description si no era null.
 */
public record TaskUpdateRequest(String user, String title, String description) {
}
