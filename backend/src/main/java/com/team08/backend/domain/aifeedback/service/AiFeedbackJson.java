package com.team08.backend.domain.aifeedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;

final class AiFeedbackJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiFeedbackJson() {
    }

    static String write(StructuredFeedback feedback) {
        try {
            return OBJECT_MAPPER.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 피드백 직렬화에 실패했습니다.", e);
        }
    }

    static StructuredFeedback read(String feedback) {
        if (feedback == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(feedback, StructuredFeedback.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 피드백 역직렬화에 실패했습니다.", e);
        }
    }
}
