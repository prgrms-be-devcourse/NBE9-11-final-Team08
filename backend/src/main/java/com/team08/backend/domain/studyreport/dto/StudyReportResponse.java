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
                report.getProgressRate(),
                report.getStudyDays(),
                parseList(objectMapper, report.getTopLectures(), new TypeReference<>() {}),
                parseList(objectMapper, report.getDailyProgress(), new TypeReference<>() {}),
                parseMap(objectMapper, report.getDailyActivityMap()),
                report.getUpdatedAt(),
                status,
                nextRegenerableAt
        );
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
