package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.study.entity.Study;

public record StudySummaryResponse(
        Long studyId,
        String title,
        String description,
        String ownerNickname
) {
    public static StudySummaryResponse from(Study study) {
        return new StudySummaryResponse(
                study.getId(),
                study.getTitle(),
                study.getDescription(),
                study.getOwnerNickname()
        );
    }
}
