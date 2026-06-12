package com.team08.backend.domain.aifeedback.dto;

import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedbackStatus;

import java.time.LocalDateTime;

public record AiFeedbackResponse(
        Long feedbackId,
        Long studyActivityId,
        AiFeedbackStatus status,
        StructuredFeedback result,
        String modelName,
        String promptVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AiFeedbackResponse from(
            AiFeedback feedback,
            StructuredFeedback result
    ) {
        return new AiFeedbackResponse(
                feedback.getId(),
                feedback.getStudyActivityId(),
                feedback.getStatus(),
                result,
                feedback.getModelName(),
                feedback.getPromptVersion(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
