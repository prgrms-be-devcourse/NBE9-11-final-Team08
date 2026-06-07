package com.team08.backend.domain.enrollment.dto;

public record CourseAccessResponse(
        Long courseId,
        boolean accessible
) {
}
