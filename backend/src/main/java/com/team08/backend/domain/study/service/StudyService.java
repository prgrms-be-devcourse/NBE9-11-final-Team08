package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional(readOnly = true)
    public List<StudySummaryResponse> getMyStudies(Long userId) {

        List<Study> studies = studyRepository.findActiveStudiesByMemberUserId(userId);

        return studies.stream()
                .map(StudySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse getStudyDetail(Long studyId, Long userId) {
        Study study = findVisibleStudyById(studyId);

        return createDetailResponse(study, userId);
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse getStudyDetailByCourseId(Long courseId, Long userId) {
        Study study = findVisibleStudyByCourseId(courseId);

        return createDetailResponse(study, userId);
    }

    private Study findVisibleStudyById(Long studyId) {
        return studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private Study findVisibleStudyByCourseId(Long courseId) {
        return studyRepository.findByCourseIdAndStatusNot(courseId, StudyStatus.DRAFT)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private StudyDetailResponse createDetailResponse(Study study, Long userId) {
        StudyMember member = findActiveMember(study.getId(), userId);

        return StudyDetailResponse.from(study, member.getRole());
    }

    private StudyMember findActiveMember(Long studyId, Long userId) {
        return studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                        studyId, userId, StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_ACCESS_DENIED));
    }
}
