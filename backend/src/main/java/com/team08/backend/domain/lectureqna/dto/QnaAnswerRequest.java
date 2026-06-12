package com.team08.backend.domain.lectureqna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QnaAnswerRequest(@NotNull Long courseId, @NotBlank String content) {
}
