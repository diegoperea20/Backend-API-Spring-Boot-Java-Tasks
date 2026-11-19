package com.backend.backendapi.dto.task;

/**
 * Fila de la consulta agregada que cuenta cuantos otros usuarios tienen un
 * titulo que tambien tiene el usuario actual.
 */
public record TitleCountRow(String title, Long numberOfTitles) {
}
