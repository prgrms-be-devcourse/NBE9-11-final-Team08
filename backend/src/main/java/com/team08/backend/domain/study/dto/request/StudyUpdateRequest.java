package com.team08.backend.domain.study.dto.request;

import com.team08.backend.domain.study.entity.StudyVisibility;

import java.time.LocalDate;

public record StudyUpdateRequest(
        String title,
        String description,
        StudyVisibility visibility,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate
) {
}
