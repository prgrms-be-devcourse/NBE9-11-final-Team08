package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lecture.entity.LectureProgress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자별 강의 수강 진행 상태")
public record ProgressResponse(
        @Schema(description = "마지막 영상 재생 위치(초)", example = "305")
        Integer lastPositionSeconds,
        @Schema(description = "누적 인정 시청 시간(초)", example = "305")
        Integer watchedSeconds,
        @Schema(description = "수강 완료 여부", example = "false")
        Boolean completed,
        @Schema(description = "진행률(%)", example = "25")
        Integer progressPercent,
        @Schema(description = "수강 완료 시각")
        LocalDateTime completedAt,
        @Schema(description = "마지막 진행 상태 갱신 시각")
        LocalDateTime updatedAt
) {
    public static ProgressResponse from(LectureProgress progress) {
        return new ProgressResponse(
                progress.getLastPositionSeconds(),
                progress.getWatchedSeconds(),
                progress.getCompleted(),
                progress.getProgressPercent(),
                progress.getCompletedAt(),
                progress.getUpdatedAt()
        );
    }
}
