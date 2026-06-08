package com.team08.backend.domain.lecture.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "강의 회고 작성/수정 요청")
public record ReflectionUpsertRequest(
        @Schema(description = "회고 내용", example = "DI 개념을 실제 프로젝트에 적용할 수 있을 것 같다.", minLength = 1, maxLength = 5000, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 5000)
        String content
) {
}
