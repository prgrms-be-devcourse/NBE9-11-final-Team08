package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.dto.request.StudyCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyUpdateRequest;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.*;
import com.team08.backend.domain.study.exception.StudyNotFoundException;
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

    private Study getOwnedStudy(Long id, Long userId) {
        Study study = studyRepository.findActiveStudyByIdWithOwner(id)
                .orElseThrow(StudyNotFoundException::new);

        study.validateOwner(userId);

        return study;
    }
}
