package com.team08.backend.domain.study.access;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyAccessContextResolver {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    public StudyAccessContext fromStudyId(Long studyId, Long userId) {
        Study study = studyRepository.findByIdWithCourse(studyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));

        return build(study, userId);
    }

    public StudyAccessContext fromCourseId(Long courseId, Long userId) {
        Study study = studyRepository.findByCourseIdWithCourse(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));

        return build(study, userId);
    }

    private StudyAccessContext build(Study study, Long userId) {
        Long studyId = study.getId();
        StudyMember studyMember = studyMemberRepository
                .findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE)
                .orElse(null);

        return new StudyAccessContext(
                studyId,
                userId,
                study.getStatus(),
                studyMember != null,
                studyMember != null ? studyMember.getRole() : null
        );
    }
}
