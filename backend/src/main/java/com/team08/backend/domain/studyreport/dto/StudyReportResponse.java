package com.team08.backend.domain.studyreport.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team08.backend.domain.studyreport.entity.StudyReport;

import java.math.BigDecimal;
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
        Map<String, Integer> dailyActivityMap
) {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static StudyReportResponse from(StudyReport report) {
        return new StudyReportResponse(
                report.getStudyId(),
                report.getTotalWatchTime(),
                report.getTotalQnaCount(),
                report.getProgressRate(),
                report.getStudyDays(),
                parseList(report.getTopLectures(), new TypeReference<>() {}),
                parseList(report.getDailyProgress(), new TypeReference<>() {}),
                parseMap(report.getDailyActivityMap())
        );
    }

    private static <T> List<T> parseList(String json, TypeReference<List<T>> type) {
        if (json == null) return List.of();
        try { return MAPPER.readValue(json, type); } catch (Exception e) { return List.of(); }
    }

    private static Map<String, Integer> parseMap(String json) {
        if (json == null) return Map.of();
        try { return MAPPER.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); }
    }
}
