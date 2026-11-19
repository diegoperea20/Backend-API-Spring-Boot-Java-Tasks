package com.backend.backendapi.dto.task;

import java.util.List;

public record SameTitleEmailsDto(String title, List<String> emails) {
}
