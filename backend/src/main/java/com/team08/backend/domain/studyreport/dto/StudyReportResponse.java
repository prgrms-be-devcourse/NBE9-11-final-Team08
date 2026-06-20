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
        LocalDateTime updatedAt
) {
    public static StudyReportResponse from(StudyReport report, ObjectMapper objectMapper) {
        return new StudyReportResponse(
                report.getStudyId(),
                report.getTotalWatchTime(),
                report.getTotalQnaCount(),
                report.getProgressRate(),
                report.getStudyDays(),
                parseList(objectMapper, report.getTopLectures(), new TypeReference<>() {}),
                parseList(objectMapper, report.getDailyProgress(), new TypeReference<>() {}),
                parseMap(objectMapper, report.getDailyActivityMap()),
                report.getUpdatedAt()
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
