package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;

public record StudyDetailResponse(
        Long studyId,
        Long courseId,
        String title,
        String description,
        StudyStatus status,
        String ownerNickname,
        StudyMemberRole myRole
) {
    public static StudyDetailResponse from(Study study, StudyMemberRole myRole) {
        return new StudyDetailResponse(
                study.getId(),
                study.getCourse().getId(),
                study.getTitle(),
                study.getDescription(),
                study.getStatus(),
                study.getOwnerNickname(),
                myRole
        );
    }
}
