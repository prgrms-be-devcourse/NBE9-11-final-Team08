package com.team08.backend.domain.lectureqna.dto;

import java.time.LocalDateTime;

public record QnaAnswerResponse(
        Long id,
        Long questionId,
        Long instructorId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
