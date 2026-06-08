package com.team08.backend.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CourseCreateRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,
        @NotBlank(message = "강의 제목은 필수입니다.")
        String title,
        String description,
        String thumbnail,
        @NotNull(message = "가격은 필수입니다.")
        @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
        Integer price
) {
}