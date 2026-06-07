package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.request.StudyApplicationCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyUpdateRequest;
import com.team08.backend.domain.study.dto.response.StudyApplicationResponse;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.*;
import com.team08.backend.domain.study.exception.*;
import com.team08.backend.domain.study.repository.StudyApplicationRepository;
import com.team08.backend.domain.study.repository.StudyMemberRepository;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyApplicationRepository studyApplicationRepository;

    @Transactional
    public Long create(Long userId, StudyCreateRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(RuntimeException::new);

        Study study = Study.create(
                owner,
                request.title(),
                request.description(),
                request.visibility(),
                request.plannedStartDate(),
                request.plannedEndDate()
        );

        Study savedStudy = studyRepository.save(study);

        StudyMember studyMember = StudyMember.createOwner(owner, study);
        studyMemberRepository.save(studyMember);

        Long studyId = savedStudy.getId();
        log.debug("스터디 생성 완료 studyId={}, ownerId={}", studyId, userId);

        return studyId;
    }

    @Transactional(readOnly = true)
    public List<StudySummaryResponse> getStudies() {
        List<Study> studies = studyRepository.findVisibleStudiesWithOwner();
        return studies.stream()
                .map(StudySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse findStudy(Long id) {
        Study study = studyRepository.findVisibleStudyByIdWithOwnerAndCourse(id)
                .orElseThrow(StudyNotFoundException::new);

        return StudyDetailResponse.from(study);
    }

    @Transactional
    public void updateStudyInfo(Long id, Long userId, StudyUpdateRequest request) {
        Study study = getOwnedStudy(id, userId);

        study.updateInfo(
                request.title(),
                request.description(),
                request.plannedStartDate(),
                request.plannedEndDate()
        );

        log.debug("스터디 변경 완료 studyId={}, ownerId={}", id, userId);
    }

    @Transactional
    public void updateStudyVisibility(Long id, Long userId, StudyVisibility visibility) {
        Study study = getOwnedStudy(id, userId);

        StudyVisibility oldVisibility = study.getVisibility();

        study.changeVisibility(visibility);

        if (oldVisibility != study.getVisibility()) {
            log.info(
                    "스터디 공개범위 변경 studyId={}, userId={}, from={}, to={}",
                    id,
                    userId,
                    oldVisibility,
                    study.getVisibility()
            );
        }
    }

    @Transactional
    public void updateStudyRecruitment(Long id, Long userId, StudyRecruitmentStatus recruitmentStatus) {
        Study study = getOwnedStudy(id, userId);

        StudyRecruitmentStatus oldRecruitmentStatus = study.getRecruitmentStatus();

        study.changeRecruitmentStatus(recruitmentStatus);

        if (oldRecruitmentStatus != study.getRecruitmentStatus()) {
            log.info(
                    "스터디 모집상태 변경 studyId={}, userId={}, from={}, to={}",
                    id,
                    userId,
                    oldRecruitmentStatus,
                    study.getRecruitmentStatus()
            );
        }
    }

    @Transactional
    public void startStudy(Long id, Long userId) {
        Study study = getOwnedStudy(id, userId);

        StudyStatus oldStatus = study.getStatus();

        study.startStudy(LocalDate.now());

        log.info(
                "스터디 시작 - 상태 변경 studyId={}, userId={}, from={}, to={}",
                id,
                userId,
                oldStatus,
                study.getStatus()
        );
    }

    @Transactional
    public void endStudy(Long id, Long userId) {
        Study study = getOwnedStudy(id, userId);

        StudyStatus oldStatus = study.getStatus();

        study.endStudy(LocalDate.now());

        log.info(
                "스터디 종료 - 상태 변경 studyId={}, userId={}, from={}, to={}",
                id,
                userId,
                oldStatus,
                study.getStatus()
        );
    }

    @Transactional
    public void deleteStudy(Long id, Long userId) {
        Study study = getOwnedStudy(id, userId);

        study.delete();

        log.debug("스터디 삭제 완료 studyId={}, userId={}", id, userId);
    }

    @Transactional
    public StudyApplicationResponse applyStudy(Long id, Long userId, StudyApplicationCreateRequest request) {
        Study study = studyRepository.findActiveStudyById(id)
                .orElseThrow(StudyNotFoundException::new);

        validateCanApply(id, userId, study);

        User user = userRepository.findById(userId)
                .orElseThrow(RuntimeException::new);

        StudyApplication application = StudyApplication.create(study, user, request.message());
        StudyApplication savedApplication = studyApplicationRepository.save(application);

        log.debug("스터디 참여 신청 완료 studyId={}, userId={}, applicationId={}", id, userId, savedApplication.getId());

        return StudyApplicationResponse.from(savedApplication);
    }

    @Transactional
    public void cancelStudyApplication(Long id, Long userId) {
        StudyApplication application = studyApplicationRepository.findByStudyIdAndUserId(id, userId)
                .orElseThrow(StudyApplicationNotFoundException::new);

        studyApplicationRepository.delete(application);

        log.debug("스터디 참여 신청 취소 studyId={}, userId={}, applicationId={}", id, userId, application.getId());
    }

    @Transactional(readOnly = true)
    public List<StudyApplicationResponse> getStudyApplications(Long id, Long userId) {
        getOwnedStudy(id, userId);

        return studyApplicationRepository.findByStudyIdOrderByAppliedAtAsc(id)
                .stream()
                .map(StudyApplicationResponse::from)
                .toList();
    }

    @Transactional
    public void approveStudyApplication(Long id, Long applicationId, Long userId) {
        getOwnedStudy(id, userId);

        StudyApplication application = getStudyApplication(id, applicationId);
        Long applicantId = application.getUser().getId();

        if (studyMemberRepository.existsByStudyIdAndUserIdAndStatus(id, applicantId, StudyMemberStatus.ACTIVE)) {
            throw new StudyAlreadyMemberException();
        }

        application.approve();

        StudyMember studyMember = StudyMember.createMember(application.getUser(), application.getStudy());
        studyMemberRepository.save(studyMember);

        log.debug("스터디 참여 신청 승인 studyId={}, applicationId={}, ownerId={}", id, applicationId, userId);
    }

    @Transactional
    public void rejectStudyApplication(Long id, Long applicationId, Long userId) {
        getOwnedStudy(id, userId);

        StudyApplication application = getStudyApplication(id, applicationId);
        application.reject();

        log.debug("스터디 참여 신청 거절 studyId={}, applicationId={}, ownerId={}", id, applicationId, userId);
    }

    private Study getOwnedStudy(Long id, Long userId) {
        Study study = studyRepository.findActiveStudyByIdWithOwner(id)
                .orElseThrow(StudyNotFoundException::new);

        study.validateOwner(userId);

        return study;
    }

    private void validateCanApply(Long id, Long userId, Study study) {
        study.validateCanReceiveApplicationFrom(userId);

        if (studyMemberRepository.existsByStudyIdAndUserIdAndStatus(id, userId, StudyMemberStatus.ACTIVE)) {
            throw new StudyAlreadyMemberException();
        }

        if (studyApplicationRepository.existsByStudyIdAndUserId(id, userId)) {
            throw new DuplicateStudyApplicationException();
        }
    }

    private StudyApplication getStudyApplication(Long id, Long applicationId) {
        return studyApplicationRepository.findByIdAndStudyId(applicationId, id)
                .orElseThrow(StudyApplicationNotFoundException::new);
    }
}
