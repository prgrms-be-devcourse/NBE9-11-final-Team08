package com.team08.backend.domain.lectureqna.dto;

import jakarta.validation.constraints.NotBlank;

public record QnaQuestionRequest(
        @NotBlank(message = "제목이 입력되지 않았습니다.")
        String title,
        @NotBlank(message = "내용이 입력되지 않았습니다.")
        String content) {
}
