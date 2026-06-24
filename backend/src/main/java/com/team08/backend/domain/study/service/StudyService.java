package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional(readOnly = true)
    public List<StudySummaryResponse> getMyStudies(Long userId) {
        List<Study> studies = studyRepository.findVisibleStudiesByUserId(userId);

        return studies.stream()
                .map(StudySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse getStudyDetail(Long studyId, Long userId) {
        Study study = findStudyById(studyId);
        validateReadableStudy(study);

        StudyMemberRole role = findActiveMember(studyId, userId)
                .map(StudyMember::getRole)
                .orElse(null);

        return createDetailResponse(study, role);
    }

    @Transactional(readOnly = true)
    public Long getStudyIdByCourseId(Long courseId) {
        Study study = findStudyByCourseId(courseId);

        return study.getId();
    }

    private Study findStudyById(Long studyId) {
        return studyRepository.findByIdWithCourse(studyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private Study findStudyByCourseId(Long courseId) {
        return studyRepository.findByCourseIdWithCourse(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private void validateReadableStudy(Study study) {
        if (study.getStatus() != StudyStatus.ACTIVE && study.getStatus() != StudyStatus.READONLY) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private StudyDetailResponse createDetailResponse(Study study, StudyMemberRole role) {
        return StudyDetailResponse.from(study, role);
    }

    private Optional<StudyMember> findActiveMember(Long studyId, Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        return studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                studyId,
                userId,
                StudyMemberStatus.ACTIVE
        );
    }
}
