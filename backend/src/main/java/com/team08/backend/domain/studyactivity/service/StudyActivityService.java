package com.team08.backend.domain.studyactivity.service;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyActivityService {

    private final StudyActivityRepository studyActivityRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public StudyActivityResponse createActivity(Long studyId, Long userId, String content) {
        Study study = findStudy(studyId);
        validateActiveStudy(study);
        validateActiveMember(studyId, userId);

        StudyActivity activity = StudyActivity.create(studyId, userId, content);
        StudyActivity savedActivity = studyActivityRepository.save(activity);

        return StudyActivityResponse.from(savedActivity);
    }

    @Transactional(readOnly = true)
    public Page<StudyActivityResponse> getActivities(
            Long studyId,
            Long userId,
            Pageable pageable
    ) {
        validateVisibleStudy(studyId);
        validateActiveMember(studyId, userId);

        Pageable latestFirstPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return studyActivityRepository
                .findAllByStudyIdAndDeletedAtIsNull(studyId, latestFirstPageable)
                .map(StudyActivityResponse::from);
    }

    @Transactional(readOnly = true)
    public StudyActivityResponse getActivity(
            Long studyId,
            Long activityId,
            Long userId
    ) {
        validateVisibleStudy(studyId);
        validateActiveMember(studyId, userId);

        StudyActivity activity = findActivity(studyId, activityId);

        return StudyActivityResponse.from(activity);
    }

    @Transactional
    public StudyActivityResponse updateActivity(
            Long studyId,
            Long activityId,
            Long userId,
            String content
    ) {
        Study study = findStudy(studyId);
        validateActiveStudy(study);
        validateActiveMember(studyId, userId);

        StudyActivity activity = findActivity(studyId, activityId);
        validateAuthor(activity, userId);
        activity.update(content);

        return StudyActivityResponse.from(activity);
    }

    private Study findStudy(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private void validateVisibleStudy(Long studyId) {
        studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private void validateActiveStudy(Study study) {
        if (study.getStatus() != StudyStatus.ACTIVE) {
            throw new CustomException(ErrorCode.STUDY_NOT_ACTIVE);
        }
    }

    private void validateActiveMember(Long studyId, Long userId) {
        boolean isActiveMember = studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId,
                userId,
                StudyMemberStatus.ACTIVE
        );

        if (!isActiveMember) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private StudyActivity findActivity(Long studyId, Long activityId) {
        return studyActivityRepository
                .findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STUDY_ACTIVITY_NOT_FOUND)
                );
    }

    private void validateAuthor(StudyActivity activity, Long userId) {
        if (!activity.getAuthorId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_ACTIVITY_ACCESS_DENIED);
        }
    }
}
