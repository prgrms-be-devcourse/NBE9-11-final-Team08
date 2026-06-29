package com.team08.backend.domain.studyactivity.service;

import com.team08.backend.domain.aifeedback.service.AiFeedbackInvalidator;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreated;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyActivityService {

    private static final String UNKNOWN_AUTHOR = "(알 수 없음)";

    private final StudyActivityRepository studyActivityRepository;
    private final AiFeedbackInvalidator aiFeedbackInvalidator;
    private final StudyAccessAuthorizer studyAccessAuthorizer;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Transactional
    public StudyActivityResponse createActivity(Long studyId, Long userId, String content) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);

        StudyActivity activity = StudyActivity.create(studyId, userId, content);
        StudyActivity savedActivity = studyActivityRepository.save(activity);

        eventPublisher.publishEvent(StudyActivityCreated.from(savedActivity));

        return StudyActivityResponse.from(savedActivity, resolveNickname(savedActivity.getAuthorId()));
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

        Page<StudyActivity> activities = studyActivityRepository
                .findAllByStudyIdAndDeletedAtIsNull(studyId, latestFirstPageable);

        Map<Long, String> nicknameByUserId = resolveNicknames(
                activities.stream().map(StudyActivity::getAuthorId).toList()
        );

        return activities.map(activity -> StudyActivityResponse.from(
                activity,
                nicknameByUserId.getOrDefault(activity.getAuthorId(), UNKNOWN_AUTHOR)
        ));
    }

    @Transactional(readOnly = true)
    public Page<StudyActivityResponse> getMyActivities(Long userId, Pageable pageable) {
        Pageable latestFirstPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<StudyActivity> activities = studyActivityRepository
                .findAllByAuthorIdAndDeletedAtIsNull(userId, latestFirstPageable);

        String authorNickname = resolveNickname(userId);

        return activities.map(activity -> StudyActivityResponse.from(activity, authorNickname));
    }

    @Transactional(readOnly = true)
    public StudyActivityResponse getActivity(
            Long studyId,
            Long activityId,
            Long userId
    ) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);

        StudyActivity activity = findActivity(studyId, activityId);

        return StudyActivityResponse.from(activity, resolveNickname(activity.getAuthorId()));
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

        return StudyActivityResponse.from(activity, resolveNickname(activity.getAuthorId()));
    }

    @Transactional
    public void deleteActivity(Long studyId, Long activityId, Long userId) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);

        StudyActivity activity = findActivity(studyId, activityId);

        activity.validateAuthor(userId);

        activity.delete();
    }

    private String resolveNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse(UNKNOWN_AUTHOR);
    }

    private Map<Long, String> resolveNicknames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    private StudyActivity findActivity(Long studyId, Long activityId) {
        return studyActivityRepository
                .findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STUDY_ACTIVITY_NOT_FOUND)
                );
    }

}
