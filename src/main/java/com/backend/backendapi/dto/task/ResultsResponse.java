package com.backend.backendapi.dto.task;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Respuesta con la misma forma que el backend FastAPI: {"results": [...]} o
 * {"message": "...", "results": []}. El mensaje se omite cuando es null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultsResponse<T>(String message, List<T> results) {

    public static <T> ResultsResponse<T> of(List<T> results) {
        return new ResultsResponse<>(null, results);
    }

    public static <T> ResultsResponse<T> withMessage(String message) {
        return new ResultsResponse<>(message, List.of());
    }
}
