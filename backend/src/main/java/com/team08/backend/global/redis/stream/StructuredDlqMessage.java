package com.team08.backend.global.redis.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

public record StructuredDlqMessage(
        String streamKey,
        String groupName,
        String recordId,
        Map<String, String> payload,
        long deliveryCount,
        LocalDateTime failedAt,
        String errorMessage
) {
    public String toJson(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // fallback if serialization fails
            return String.format("{\"streamKey\":\"%s\",\"recordId\":\"%s\",\"error\":\"Serialization Failed\"}", streamKey, recordId);
        }
    }
}
