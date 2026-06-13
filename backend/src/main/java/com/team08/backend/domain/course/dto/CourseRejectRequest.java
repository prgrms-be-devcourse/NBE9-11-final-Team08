package com.team08.backend.domain.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseRejectRequest(
        @NotBlank(message = "반려 사유는 필수입니다.")
        String reason
) {
}