package com.team08.backend.domain.studyreport.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

final class StudyReportJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private StudyReportJson() {}

    static <T> String write(T value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("StudyReport 직렬화 실패", e);
        }
    }

    static <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null) return List.of();
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("StudyReport 역직렬화 실패", e);
        }
    }

    static Map<String, Integer> readMap(String json) {
        if (json == null) return Map.of();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("StudyReport 역직렬화 실패", e);
        }
    }
}
