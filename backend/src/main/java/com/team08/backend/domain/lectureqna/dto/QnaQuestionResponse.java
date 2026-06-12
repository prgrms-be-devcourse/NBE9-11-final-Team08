package com.team08.backend.domain.lectureqna.dto;

import java.time.LocalDateTime;

public record QnaQuestionResponse(
        Long id,
        Long lectureId,
        Long userId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        QnaAnswerSummary answer
) {
}
