package com.team08.backend.domain.lecture.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "강의 영상 재생 위치 갱신 요청")
public record ProgressUpdateRequest(
        @Schema(description = "마지막 영상 재생 위치(초)", example = "305", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0)
        Integer positionSeconds,

        @Schema(description = "강제 수강 완료 처리 여부. true면 위치와 관계없이 완료 처리합니다.", example = "false")
        Boolean completed
) {
}
