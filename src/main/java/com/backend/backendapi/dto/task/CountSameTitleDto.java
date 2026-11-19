package com.backend.backendapi.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountSameTitleDto(
        @JsonProperty("Number of titles") Long numberOfTitles,
        String title
) {

    public static CountSameTitleDto from(TitleCountRow row) {
        return new CountSameTitleDto(row.numberOfTitles(), row.title());
    }
}
