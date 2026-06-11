package com.team08.backend.domain.lectureqna.dto;

import jakarta.validation.constraints.NotBlank;

public record QnaAnswerRequest(@NotBlank String content) {
}
