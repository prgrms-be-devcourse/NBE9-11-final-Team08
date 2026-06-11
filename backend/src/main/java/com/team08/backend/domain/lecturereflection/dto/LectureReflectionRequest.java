package com.team08.backend.domain.lecturereflection.dto;

import jakarta.validation.constraints.NotBlank;

public record LectureReflectionRequest(@NotBlank String content) {
}
