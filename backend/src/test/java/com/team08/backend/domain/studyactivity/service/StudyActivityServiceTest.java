package com.team08.backend.domain.studyactivity.service;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.fixture.StudyFixture;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyActivityServiceTest {

    @Mock
    private StudyActivityRepository studyActivityRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

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
}
