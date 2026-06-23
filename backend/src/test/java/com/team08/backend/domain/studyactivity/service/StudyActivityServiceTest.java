package com.team08.backend.domain.studyactivity.service;

import com.team08.backend.domain.aifeedback.service.AiFeedbackInvalidator;
import com.team08.backend.domain.feed.outbox.FeedOutboxService;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyActivityServiceTest {

    @Mock
    private StudyActivityRepository studyActivityRepository;

    @Mock
    private AiFeedbackInvalidator aiFeedbackInvalidator;

    @Mock
    private FeedOutboxService feedOutboxService;

    @Mock
    private StudyAccessAuthorizer studyAccessAuthorizer;

    @InjectMocks
    private StudyActivityService studyActivityService;

    @Test
    void 스터디_활동을_생성한다() {
        Long studyId = 1L;
        Long userId = 2L;
        String content = "오늘 학습한 내용을 스터디원들과 공유합니다.";
        StudyActivity savedActivity = activity(100L, studyId, userId);

        given(studyActivityRepository.save(any(StudyActivity.class)))
                .willReturn(savedActivity);

        StudyActivityResponse result =
                studyActivityService.createActivity(studyId, userId, content);

        ArgumentCaptor<StudyActivity> captor = ArgumentCaptor.forClass(StudyActivity.class);
        verify(studyActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getStudyId()).isEqualTo(studyId);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(userId);
        assertThat(captor.getValue().getContent()).isEqualTo(content);
        assertThat(result.activityId()).isEqualTo(100L);
        assertThat(result.studyId()).isEqualTo(studyId);
        assertThat(result.authorId()).isEqualTo(userId);

        verify(studyAccessAuthorizer)
                .authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);
        verify(feedOutboxService).createStudyActivityCreatedEvent(savedActivity);
    }

    @Test
    void 스터디_활동_목록을_최신순으로_조회한다() {
        Long studyId = 1L;
        Long userId = 2L;
        Pageable requestPageable = PageRequest.of(
                1, 5, Sort.by(Sort.Direction.ASC, "content")
        );
        Pageable expectedPageable = PageRequest.of(
                1,
                5,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        StudyActivity activity = activity(100L, studyId, userId);

        given(studyActivityRepository.findAllByStudyIdAndDeletedAtIsNull(
                studyId, expectedPageable
        )).willReturn(new PageImpl<>(List.of(activity), expectedPageable, 1));

        Page<StudyActivityResponse> result =
                studyActivityService.getActivities(studyId, userId, requestPageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).activityId()).isEqualTo(100L);
        verify(studyAccessAuthorizer)
                .authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);
        verify(studyActivityRepository)
                .findAllByStudyIdAndDeletedAtIsNull(studyId, expectedPageable);
    }

    @Test
    void 활동이_없으면_빈_페이지를_반환한다() {
        Long studyId = 1L;
        Long userId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        given(studyActivityRepository.findAllByStudyIdAndDeletedAtIsNull(
                eq(studyId), any(Pageable.class)
        )).willReturn(Page.empty());

        Page<StudyActivityResponse> result =
                studyActivityService.getActivities(studyId, userId, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void 스터디_활동_상세를_조회한다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        Long authorId = 3L;
        StudyActivity activity = activity(activityId, studyId, authorId);

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        StudyActivityResponse result =
                studyActivityService.getActivity(studyId, activityId, userId);

        assertThat(result.activityId()).isEqualTo(activityId);
        assertThat(result.studyId()).isEqualTo(studyId);
        assertThat(result.authorId()).isEqualTo(authorId);
        verify(studyAccessAuthorizer)
                .authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);
    }

    @Test
    void 존재하지_않거나_삭제된_활동_상세는_조회할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.getActivity(studyId, activityId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_NOT_FOUND);
    }

    @Test
    void 작성자가_활동을_수정한다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        String updatedContent = "수정한 이후의 스터디 활동 내용입니다.";
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        StudyActivityResponse result = studyActivityService.updateActivity(
                studyId, activityId, userId, updatedContent
        );

        assertThat(activity.getContent()).isEqualTo(updatedContent);
        assertThat(result.content()).isEqualTo(updatedContent);
        verify(studyAccessAuthorizer)
                .authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);
        verify(aiFeedbackInvalidator).markStale(activityId);
    }

    @Test
    void 존재하지_않거나_삭제된_활동은_수정할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyActivityService.updateActivity(
                studyId, activityId, userId, "수정한 이후의 스터디 활동 내용입니다."
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_NOT_FOUND);
        verify(aiFeedbackInvalidator, never()).markStale(any());
    }

    @Test
    void 작성자가_아니면_활동을_수정할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        StudyActivity activity = activity(activityId, studyId, 3L);

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        assertThatThrownBy(() -> studyActivityService.updateActivity(
                studyId, activityId, userId, "수정한 이후의 스터디 활동 내용입니다."
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_ACCESS_DENIED);
        assertThat(activity.getContent())
                .isEqualTo("오늘 학습한 내용을 스터디원들과 공유합니다.");
        verify(aiFeedbackInvalidator, never()).markStale(any());
    }

    @Test
    void 작성자가_활동을_삭제한다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        studyActivityService.deleteActivity(studyId, activityId, userId);

        assertThat(activity.getDeletedAt()).isNotNull();
        verify(studyAccessAuthorizer)
                .authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);
    }

    @Test
    void 존재하지_않거나_삭제된_활동은_삭제할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.deleteActivity(studyId, activityId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_NOT_FOUND);
    }

    @Test
    void 작성자가_아니면_활동을_삭제할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        StudyActivity activity = activity(activityId, studyId, 3L);

        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        assertThatThrownBy(() ->
                studyActivityService.deleteActivity(studyId, activityId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_ACCESS_DENIED);
        assertThat(activity.getDeletedAt()).isNull();
    }

    private StudyActivity activity(Long activityId, Long studyId, Long authorId) {
        StudyActivity activity = StudyActivity.create(
                studyId,
                authorId,
                "오늘 학습한 내용을 스터디원들과 공유합니다."
        );
        ReflectionTestUtils.setField(activity, "id", activityId);
        ReflectionTestUtils.setField(
                activity, "createdAt", LocalDateTime.of(2026, 6, 12, 20, 0)
        );
        return activity;
    }
}
