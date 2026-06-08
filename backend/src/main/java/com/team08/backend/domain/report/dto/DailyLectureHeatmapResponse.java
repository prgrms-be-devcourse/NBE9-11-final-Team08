package com.team08.backend.domain.report.dto;

import com.team08.backend.domain.report.entity.DailyLectureStat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "일별 수강 완료 강의 수 히트맵 데이터")
public record DailyLectureHeatmapResponse(
        @Schema(description = "날짜", example = "2026-06-07")
        LocalDate date,
        @Schema(description = "해당 날짜의 수강 완료 강의 수", example = "3")
        Integer completedLectureCount
) {
    public static DailyLectureHeatmapResponse from(DailyLectureStat stat) {
        return new DailyLectureHeatmapResponse(stat.getStatDate(), stat.getCompletedLectureCount());
    }
}
