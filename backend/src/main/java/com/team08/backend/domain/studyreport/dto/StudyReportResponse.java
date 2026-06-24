package com.team08.backend.domain.studyreport.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.studyreport.entity.StudyReport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record StudyReportResponse(
        Long studyId,
        Integer totalWatchTime,
        Integer totalQnaCount,
        BigDecimal progressRate,
        Integer studyDays,
        List<TopLectureEntry> topLectures,
        List<DailyProgressEntry> dailyProgress,
        Map<String, Integer> dailyActivityMap,
        LocalDateTime updatedAt,
        /** 이 응답이 조회(LOADED)/갱신(REGENERATED)/갱신 불가(COOLDOWN) 중 무엇인지. */
        ReportStatus status,
        /** 다음 재집계가 가능한 시각(쿨다운 해제 시점). 이 시각 이전 refresh 요청은 COOLDOWN 처리된다. */
        LocalDateTime nextRegenerableAt
) {
    public static StudyReportResponse from(
            StudyReport report, ReportStatus status, LocalDateTime nextRegenerableAt, ObjectMapper objectMapper) {
        return new StudyReportResponse(
                report.getStudyId(),
                report.getTotalWatchTime(),
                report.getTotalQnaCount(),
                progressRate(report),
                report.getStudyDays(),
                parseList(objectMapper, report.getTopLectures(), new TypeReference<>() {}),
                parseList(objectMapper, report.getDailyProgress(), new TypeReference<>() {}),
                parseMap(objectMapper, report.getDailyActivityMap()),
                report.getUpdatedAt(),
                status,
                nextRegenerableAt
        );
    }

    /** 진행률(0~100, 소수2자리)은 저장하지 않고 완료/전체 강의 수에서 파생한다. */
    private static BigDecimal progressRate(StudyReport report) {
        int total = report.getTotalLectures() == null ? 0 : report.getTotalLectures();
        int completed = report.getCompletedLectures() == null ? 0 : report.getCompletedLectures();
        if (total == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(completed)
                .divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static <T> List<T> parseList(ObjectMapper mapper, String json, TypeReference<List<T>> type) {
        if (json == null) return List.of();
        try { return mapper.readValue(json, type); } catch (Exception e) { return List.of(); }
    }

    private static Map<String, Integer> parseMap(ObjectMapper mapper, String json) {
        if (json == null) return Map.of();
        try { return mapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); }
    }
}
