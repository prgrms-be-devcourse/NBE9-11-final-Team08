package com.team08.backend.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "스터디 종료 후 발급되는 사용자 학습 리포트")
public record StudyReportResponse(
        @Schema(description = "스터디 ID", example = "1")
        Long studyId,
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "코스 ID", example = "1")
        Long courseId,
        @Schema(description = "총 강의 시청 시간(초)", example = "3600")
        Integer totalWatchTimeSeconds,
        @Schema(description = "총 작성 댓글 수", example = "7")
        Long totalComments,
        @Schema(description = "수강 완료 강의 수", example = "8")
        Long completedLectures,
        @Schema(description = "전체 강의 수", example = "10")
        Long totalLectures,
        @Schema(description = "수강 진행률(%)", example = "80.00")
        BigDecimal progressRate,
        @Schema(description = "일별 수강 완료 강의 수 히트맵")
        List<DailyLectureHeatmapResponse> heatmap,
        @Schema(description = "리포트 발급 시각")
        LocalDateTime generatedAt
) {
}
