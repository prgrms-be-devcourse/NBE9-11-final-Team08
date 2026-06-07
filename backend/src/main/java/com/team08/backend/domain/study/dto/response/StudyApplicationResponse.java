package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.study.entity.ApplicationStatus;
import com.team08.backend.domain.study.entity.StudyApplication;

import java.time.LocalDateTime;

public record StudyApplicationResponse(
        Long applicationId,
        Long studyId,
        Long userId,
        String message,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime processedAt
) {
    public static StudyApplicationResponse from(StudyApplication application) {
        return new StudyApplicationResponse(
                application.getId(),
                application.getStudy().getId(),
                application.getUser().getId(),
                application.getMessage(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getProcessedAt()
        );
    }
}
