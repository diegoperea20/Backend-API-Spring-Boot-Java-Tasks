package com.backend.backendapi.dto.task;

/**
 * Fila de la consulta que relaciona un titulo con cada usuario (email) que lo
 * tiene, excluyendo al usuario consultado.
 */
public record TitleUserRow(String title, String user) {
}
