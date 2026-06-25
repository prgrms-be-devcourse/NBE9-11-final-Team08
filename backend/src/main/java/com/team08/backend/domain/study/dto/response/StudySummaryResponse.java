package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.study.entity.Study;

public record StudySummaryResponse(
        Long studyId,
        String title,
        String description,
        String ownerNickname,
        int progressRate,
        int completedLectures,
        int totalLectures
) {
    public static StudySummaryResponse from(Study study, int completedLectures, int totalLectures) {
        int progressRate = totalLectures == 0 ? 0
                : (int) Math.round((double) completedLectures * 100 / totalLectures);
        return new StudySummaryResponse(
                study.getId(),
                study.getTitle(),
                study.getDescription(),
                study.getOwnerNickname(),
                progressRate,
                completedLectures,
                totalLectures
        );
    }
}
