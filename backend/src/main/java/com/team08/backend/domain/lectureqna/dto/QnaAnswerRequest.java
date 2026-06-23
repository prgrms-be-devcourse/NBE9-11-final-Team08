package com.team08.backend.domain.lectureqna.dto;

import jakarta.validation.constraints.NotBlank;

public record QnaAnswerRequest(
        @NotBlank(message = "응답이 입력되지 않았습니다.")
        String content) {
}
