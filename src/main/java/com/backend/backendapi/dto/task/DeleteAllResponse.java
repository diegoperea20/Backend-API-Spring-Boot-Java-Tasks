package com.backend.backendapi.dto.task;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * FastAPI devolvia {"message": "...", "tasks": []} cuando no habia tareas que
 * borrar, y un array plano cuando si las habia.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeleteAllResponse(String message, List<TaskDto> tasks) {
}
