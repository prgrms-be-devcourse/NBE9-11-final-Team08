package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyRecruitmentStatus;
import com.team08.backend.domain.study.entity.StudyStatus;

import java.time.LocalDate;

public record StudySummaryResponse(
        Long id,
        String title,
        String ownerNickname,
        StudyStatus status,
        StudyRecruitmentStatus recruitmentStatus,
        LocalDate startDate
) {
    public static StudySummaryResponse from(Study study) {
        return new StudySummaryResponse(
                study.getId(),
                study.getTitle(),
                study.getOwner().getNickname(),
                study.getStatus(),
                study.getRecruitmentStatus(),
                study.getStartDate()
        );
    }
}
