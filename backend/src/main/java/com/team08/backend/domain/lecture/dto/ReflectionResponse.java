package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lecture.entity.LectureReflection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "강의 회고 응답")
public record ReflectionResponse(
        @Schema(description = "회고 ID", example = "1")
        Long id,
        @Schema(description = "회고 내용", example = "DI 개념을 실제 프로젝트에 적용할 수 있을 것 같다.")
        String content,
        @Schema(description = "작성 시각")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
    public static ReflectionResponse from(LectureReflection reflection) {
        if (reflection == null) {
            return null;
        }
        return new ReflectionResponse(
                reflection.getId(),
                reflection.getContent(),
                reflection.getCreatedAt(),
                reflection.getUpdatedAt()
        );
    }
}
