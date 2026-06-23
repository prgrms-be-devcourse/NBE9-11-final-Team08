package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
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
    private final StudyAccessAuthorizer studyAccessAuthorizer;

    @Transactional(readOnly = true)
    public List<StudySummaryResponse> getMyStudies(Long userId) {
        List<Study> studies = studyRepository.findVisibleStudiesByUserId(userId);

        return studies.stream()
                .map(StudySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse getStudyDetail(Long studyId, Long userId) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);

        Study study = findStudyById(studyId);

        return createDetailResponse(study, userId);
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse getStudyDetailByCourseId(Long courseId, Long userId) {
        studyAccessAuthorizer.authorizeByCourseId(courseId, userId, StudyAction.VIEW_STUDY_CONTENT);

        Study study = findStudyByCourseId(courseId);

        return createDetailResponse(study, userId);
    }

    private Study findStudyById(Long studyId) {
        return studyRepository.findByIdWithCourse(studyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private Study findStudyByCourseId(Long courseId) {
        return studyRepository.findByCourseIdWithCourse(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private StudyDetailResponse createDetailResponse(Study study, Long userId) {
        StudyMember member = findMember(study.getId(), userId);

        return StudyDetailResponse.from(study, member.getRole());
    }

    private StudyMember findMember(Long studyId, Long userId) {
        return studyMemberRepository.findByStudyIdAndUserId(
                        studyId, userId
                )
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_MEMBER_NOT_FOUND));
    }
}
