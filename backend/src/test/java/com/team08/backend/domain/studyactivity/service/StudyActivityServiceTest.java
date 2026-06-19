package com.team08.backend.domain.studyactivity.service;

import com.team08.backend.domain.aifeedback.service.AiFeedbackInvalidator;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.fixture.StudyFixture;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreatedEvent;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyActivityServiceTest {

    @Mock
    private StudyActivityRepository studyActivityRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private AiFeedbackInvalidator aiFeedbackInvalidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StudyActivityService studyActivityService;

    @Test
    void ACTIVE_스터디의_ACTIVE_멤버가_활동을_생성한다() {
        Long studyId = 1L;
        Long userId = 2L;
        String content = "오늘 학습한 내용을 스터디원들과 공유합니다.";
        Study study = StudyFixture.activeStudy();
        StudyActivity savedActivity = StudyActivity.create(studyId, userId, content);
        ReflectionTestUtils.setField(savedActivity, "id", 100L);
        ReflectionTestUtils.setField(
                savedActivity, "createdAt", LocalDateTime.of(2026, 6, 12, 20, 0)
        );

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.save(org.mockito.ArgumentMatchers.any(StudyActivity.class)))
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
    }

    @Test
    void 존재하지_않는_스터디에는_활동을_생성할_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findById(studyId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.createActivity(studyId, 1L, "오늘 학습 내용을 충분히 길게 작성합니다."))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );
        verify(studyActivityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void DRAFT_스터디에는_활동을_생성할_수_없다() {
        assertInactiveStudyCannotCreate(StudyFixture.draftStudy());
    }

    @Test
    void READONLY_스터디에는_활동을_생성할_수_없다() {
        Study study = StudyFixture.activeStudy();
        ReflectionTestUtils.setField(study, "status", StudyStatus.READONLY);

        assertInactiveStudyCannotCreate(study);
    }

    @Test
    void 가입하지_않은_사용자는_활동을_생성할_수_없다() {
        assertNonActiveMemberCannotCreate(2L);
    }

    @Test
    void 탈퇴한_멤버는_활동을_생성할_수_없다() {
        assertNonActiveMemberCannotCreate(3L);
    }

    @Test
    void 강퇴된_멤버는_활동을_생성할_수_없다() {
        assertNonActiveMemberCannotCreate(4L);
    }

    @Test
    void ACTIVE_스터디의_ACTIVE_멤버가_활동_목록을_최신순으로_조회한다() {
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
        Study study = StudyFixture.activeStudy();
        StudyActivity activity = activity(100L, studyId, userId);

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findAllByStudyIdAndDeletedAtIsNull(
                studyId, expectedPageable
        )).willReturn(new PageImpl<>(List.of(activity), expectedPageable, 1));

        Page<StudyActivityResponse> result =
                studyActivityService.getActivities(studyId, userId, requestPageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).activityId()).isEqualTo(100L);
        verify(studyActivityRepository)
                .findAllByStudyIdAndDeletedAtIsNull(studyId, expectedPageable);
    }

    @Test
    void READONLY_스터디의_ACTIVE_멤버도_활동_목록을_조회할_수_있다() {
        Long studyId = 1L;
        Long userId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Study study = StudyFixture.activeStudy();
        ReflectionTestUtils.setField(study, "status", StudyStatus.READONLY);

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findAllByStudyIdAndDeletedAtIsNull(
                eq(studyId), any(Pageable.class)
        )).willReturn(Page.empty());

        Page<StudyActivityResponse> result =
                studyActivityService.getActivities(studyId, userId, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void 활동이_없으면_빈_페이지를_반환한다() {
        Long studyId = 1L;
        Long userId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findAllByStudyIdAndDeletedAtIsNull(
                eq(studyId), any(Pageable.class)
        )).willReturn(Page.empty());

        Page<StudyActivityResponse> result =
                studyActivityService.getActivities(studyId, userId, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void 존재하지_않는_스터디의_활동_목록은_조회할_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.getActivities(
                        studyId, 1L, PageRequest.of(0, 10)
                ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(
                        any(), any(), any()
                );
        verify(studyActivityRepository, never())
                .findAllByStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void DRAFT_스터디의_활동_목록은_조회할_수_없다() {
        Long studyId = 1L;

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.getActivities(
                        studyId, 1L, PageRequest.of(0, 10)
                ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);
    }

    @Test
    void ACTIVE_멤버가_아니면_활동_목록을_조회할_수_없다() {
        Long studyId = 1L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(false);

        assertThatThrownBy(() ->
                studyActivityService.getActivities(
                        studyId, userId, PageRequest.of(0, 10)
                ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);

        verify(studyActivityRepository, never())
                .findAllByStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void ACTIVE_스터디의_ACTIVE_멤버가_활동_상세를_조회한다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        StudyActivityResponse result =
                studyActivityService.getActivity(studyId, activityId, userId);

        assertThat(result.activityId()).isEqualTo(activityId);
        assertThat(result.studyId()).isEqualTo(studyId);
        assertThat(result.authorId()).isEqualTo(userId);
    }

    @Test
    void READONLY_스터디의_ACTIVE_멤버도_활동_상세를_조회할_수_있다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();
        ReflectionTestUtils.setField(study, "status", StudyStatus.READONLY);
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        StudyActivityResponse result =
                studyActivityService.getActivity(studyId, activityId, userId);

        assertThat(result.activityId()).isEqualTo(activityId);
    }

    @Test
    void 존재하지_않는_스터디의_활동_상세는_조회할_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.getActivity(studyId, 100L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(any(), any(), any());
        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void DRAFT_스터디의_활동_상세는_조회할_수_없다() {
        Long studyId = 1L;

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.getActivity(studyId, 100L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);

        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void ACTIVE_멤버가_아니면_활동_상세를_조회할_수_없다() {
        Long studyId = 1L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(false);

        assertThatThrownBy(() ->
                studyActivityService.getActivity(studyId, 100L, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);

        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 존재하지_않는_활동_상세는_조회할_수_없다() {
        assertActivityNotFound(1L, 100L);
    }

    @Test
    void 삭제된_활동_상세는_조회할_수_없다() {
        assertActivityNotFound(1L, 101L);
    }

    @Test
    void 다른_스터디의_활동_상세는_조회할_수_없다() {
        assertActivityNotFound(1L, 102L);
    }

    @Test
    void ACTIVE_스터디의_ACTIVE_멤버인_작성자가_활동을_수정한다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        String updatedContent = "수정한 이후의 스터디 활동 내용입니다.";
        Study study = StudyFixture.activeStudy();
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        StudyActivityResponse result = studyActivityService.updateActivity(
                studyId, activityId, userId, updatedContent
        );

        assertThat(activity.getContent()).isEqualTo(updatedContent);
        assertThat(result.content()).isEqualTo(updatedContent);
        verify(aiFeedbackInvalidator).markStale(activityId);
    }

    @Test
    void 존재하지_않는_스터디에서는_활동을_수정할_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findById(studyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyActivityService.updateActivity(
                studyId, 100L, 1L, "수정한 이후의 스터디 활동 내용입니다."
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(any(), any(), any());
        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void DRAFT_스터디에서는_활동을_수정할_수_없다() {
        assertInactiveStudyCannotUpdate(StudyFixture.draftStudy());
    }

    @Test
    void READONLY_스터디에서는_활동을_수정할_수_없다() {
        Study study = StudyFixture.activeStudy();
        ReflectionTestUtils.setField(study, "status", StudyStatus.READONLY);

        assertInactiveStudyCannotUpdate(study);
    }

    @Test
    void ACTIVE_멤버가_아니면_활동을_수정할_수_없다() {
        Long studyId = 1L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(false);

        assertThatThrownBy(() -> studyActivityService.updateActivity(
                studyId, 100L, userId, "수정한 이후의 스터디 활동 내용입니다."
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);

        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 삭제되었거나_다른_스터디의_활동은_수정할_수_없다() {
        Long studyId = 1L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                100L, studyId
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyActivityService.updateActivity(
                studyId, 100L, userId, "수정한 이후의 스터디 활동 내용입니다."
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_NOT_FOUND);
    }

    @Test
    void 작성자가_아니면_활동을_수정할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();
        StudyActivity activity = activity(activityId, studyId, 3L);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
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
    }

    @Test
    void ACTIVE_스터디의_ACTIVE_멤버인_작성자가_활동을_삭제한다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.of(activity));

        studyActivityService.deleteActivity(studyId, activityId, userId);

        assertThat(activity.getDeletedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_스터디에서는_활동을_삭제할_수_없다() {
        Long studyId = 999L;

        given(studyRepository.findById(studyId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.deleteActivity(studyId, 100L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_FOUND);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(any(), any(), any());
        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void DRAFT_스터디에서는_활동을_삭제할_수_없다() {
        assertInactiveStudyCannotDelete(StudyFixture.draftStudy());
    }

    @Test
    void READONLY_스터디에서는_활동을_삭제할_수_없다() {
        Study study = StudyFixture.activeStudy();
        ReflectionTestUtils.setField(study, "status", StudyStatus.READONLY);

        assertInactiveStudyCannotDelete(study);
    }

    @Test
    void ACTIVE_멤버가_아니면_활동을_삭제할_수_없다() {
        Long studyId = 1L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(false);

        assertThatThrownBy(() ->
                studyActivityService.deleteActivity(studyId, 100L, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);

        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 삭제되었거나_다른_스터디의_활동은_삭제할_수_없다() {
        Long studyId = 1L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                100L, studyId
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.deleteActivity(studyId, 100L, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_NOT_FOUND);
    }

    @Test
    void 작성자가_아니면_활동을_삭제할_수_없다() {
        Long studyId = 1L;
        Long activityId = 100L;
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();
        StudyActivity activity = activity(activityId, studyId, 3L);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
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

    @Test
    void createActivity_성공하면_StudyActivityCreatedEvent를_발행한다() {
        // given
        Study study = StudyFixture.activeStudy();
        User user = UserFixture.builder().build();
        Long activityId = 100L;
        Long studyId = study.getId();
        Long userId = user.getId();
        StudyActivity activity = activity(activityId, studyId, userId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.save(
                any(StudyActivity.class)
        )).willReturn(activity);

        // when
        studyActivityService.createActivity(studyId, userId, activity.getContent());

        // then
        ArgumentCaptor<StudyActivityCreatedEvent> captor =
                ArgumentCaptor.forClass(StudyActivityCreatedEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        StudyActivityCreatedEvent event = captor.getValue();

        assertThat(event.studyActivityId()).isEqualTo(activity.getId());
        assertThat(event.studyId()).isEqualTo(studyId);
        assertThat(event.authorId()).isEqualTo(userId);
        assertThat(event.content()).isEqualTo(activity.getContent());
    }

    private void assertInactiveStudyCannotCreate(Study study) {
        Long studyId = study.getId();
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        assertThatThrownBy(() ->
                studyActivityService.createActivity(studyId, 1L, "오늘 학습 내용을 충분히 길게 작성합니다."))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_ACTIVE);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );
        verify(studyActivityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void assertNonActiveMemberCannotCreate(Long userId) {
        Long studyId = 1L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(false);

        assertThatThrownBy(() ->
                studyActivityService.createActivity(studyId, userId, "오늘 학습 내용을 충분히 길게 작성합니다."))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);

        verify(studyActivityRepository, never()).save(org.mockito.ArgumentMatchers.any());
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

    private void assertActivityNotFound(Long studyId, Long activityId) {
        Long userId = 2L;
        Study study = StudyFixture.activeStudy();

        given(studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                activityId, studyId
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                studyActivityService.getActivity(studyId, activityId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACTIVITY_NOT_FOUND);
    }

    private void assertInactiveStudyCannotUpdate(Study study) {
        Long studyId = study.getId();
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        assertThatThrownBy(() -> studyActivityService.updateActivity(
                studyId, 100L, 1L, "수정한 이후의 스터디 활동 내용입니다."
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_ACTIVE);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(any(), any(), any());
        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }

    private void assertInactiveStudyCannotDelete(Study study) {
        Long studyId = study.getId();
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        assertThatThrownBy(() ->
                studyActivityService.deleteActivity(studyId, 100L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_NOT_ACTIVE);

        verify(studyMemberRepository, never())
                .existsByStudyIdAndUserIdAndStatus(any(), any(), any());
        verify(studyActivityRepository, never())
                .findByIdAndStudyIdAndDeletedAtIsNull(any(), any());
    }
}
