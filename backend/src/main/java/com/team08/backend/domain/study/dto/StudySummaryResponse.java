package com.team08.backend.domain.study.dto;

public record StudySummaryResponse(
        Long studyId,
        String title,
        String description
) {
}
