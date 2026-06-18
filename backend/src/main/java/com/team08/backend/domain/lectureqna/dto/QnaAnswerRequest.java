package com.team08.backend.domain.lectureqna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QnaAnswerRequest(
        @NotNull(message = "강좌 id가 누락되었습니다.")
        Long courseId,
        @NotBlank(message = "응답이 입력되지 않았습니다.")
        String content) {
}
