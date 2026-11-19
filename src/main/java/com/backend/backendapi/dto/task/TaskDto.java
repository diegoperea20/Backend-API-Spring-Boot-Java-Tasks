package com.backend.backendapi.dto.task;

import com.backend.backendapi.domain.Task;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        String user,
        String title,
        String description,
        @JsonProperty("created_at") LocalDateTime createdAt
) {

    public static TaskDto from(Task task) {
        return new TaskDto(task.getId(), task.getUser(), task.getTitle(), task.getDescription(), task.getCreatedAt());
    }
}
