package com.team08.backend.domain.lecture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "러닝스페이스 진입 응답")
public record LearningSpaceResponse(
        @Schema(description = "강의 정보")
        LectureSummaryResponse lecture,
        @Schema(description = "사용자 수강 진행 상태")
        ProgressResponse progress,
        @Schema(description = "사용자 회고. 작성하지 않은 경우 null")
        ReflectionResponse reflection
) {
}
