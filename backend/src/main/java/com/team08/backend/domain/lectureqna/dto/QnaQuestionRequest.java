package com.team08.backend.domain.lectureqna.dto;

import jakarta.validation.constraints.NotBlank;

public record QnaQuestionRequest(@NotBlank String title, @NotBlank String content) {
}
