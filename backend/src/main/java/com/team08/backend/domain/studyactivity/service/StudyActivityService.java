package com.team08.backend.domain.studyactivity.service;

import com.team08.backend.domain.aifeedback.service.AiFeedbackInvalidator;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreated;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final AiFeedbackInvalidator aiFeedbackInvalidator;
    private final StudyAccessAuthorizer studyAccessAuthorizer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public StudyActivityResponse createActivity(Long studyId, Long userId, String content) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);

        StudyActivity activity = StudyActivity.create(studyId, userId, content);
        StudyActivity savedActivity = studyActivityRepository.save(activity);

        eventPublisher.publishEvent(StudyActivityCreated.from(savedActivity));

        return StudyActivityResponse.from(savedActivity);
    }

    @Transactional(readOnly = true)
    public Page<StudyActivityResponse> getActivities(
            Long studyId,
            Long userId,
            Pageable pageable
    ) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);

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
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);

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
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);

        StudyActivity activity = findActivity(studyId, activityId);

        activity.validateAuthor(userId);

        activity.update(content);

        aiFeedbackInvalidator.markStale(activityId);

        return StudyActivityResponse.from(activity);
    }

    @Transactional
    public void deleteActivity(Long studyId, Long activityId, Long userId) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);

        StudyActivity activity = findActivity(studyId, activityId);

        activity.validateAuthor(userId);

        activity.delete();
    }

    private StudyActivity findActivity(Long studyId, Long activityId) {
        return studyActivityRepository
                .findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STUDY_ACTIVITY_NOT_FOUND)
                );
    }

}
