package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyRecruitmentStatus;
import com.team08.backend.domain.study.entity.StudyStatus;

import java.time.LocalDate;

public record StudyDetailResponse(
        Long id,
        String title,
        String ownerNickname,
        Long courseId,
        StudyStatus status,
        StudyRecruitmentStatus recruitmentStatus,
        LocalDate startDate,
        LocalDate endDate
) {
    public static StudyDetailResponse from(Study study) {
        Long courseId = 0L;
        Course course = study.getCourse();
        if (course != null) {
            courseId = course.getId();
        }

        return new StudyDetailResponse(
                study.getId(),
                study.getTitle(),
                study.getOwner().getNickname(),
                courseId,
                study.getStatus(),
                study.getRecruitmentStatus(),
                study.getStartDate(),
                study.getEndDate()
        );
    }
}
