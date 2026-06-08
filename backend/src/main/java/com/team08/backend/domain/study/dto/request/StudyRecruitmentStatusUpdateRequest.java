package com.team08.backend.domain.study.dto.request;

import com.team08.backend.domain.study.entity.StudyRecruitmentStatus;

public record StudyRecruitmentStatusUpdateRequest(
        StudyRecruitmentStatus recruitmentStatus
) {
}
