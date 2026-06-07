package com.team08.backend.domain.study.service;

import com.team08.backend.domain.fixture.StudyFixture;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.study.dto.request.StudyCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyUpdateRequest;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.*;
import com.team08.backend.domain.study.exception.InvalidStudyPeriodException;
import com.team08.backend.domain.study.exception.StudyAccessDeniedException;
import com.team08.backend.domain.study.exception.StudyNotFoundException;
import com.team08.backend.domain.study.repository.StudyMemberRepository;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StudyServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private StudyService studyService;

    @Test
    void 스터디를_생성한다() {
        // given
        Long userId = 1L;
        User owner = UserFixture.user(userId);

        StudyCreateRequest request = new StudyCreateRequest(
                "스프링 스터디",
                "스프링 공부",
                StudyVisibility.PUBLIC,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        Study study = Study.create(
                owner,
                request.title(),
                request.description(),
                request.visibility(),
                request.plannedStartDate(),
                request.plannedEndDate()
        );

        StudyMember studyMember = StudyMember.createOwner(owner, study);

        ReflectionTestUtils.setField(study, "id", 10L);

        given(userRepository.findById(userId)).willReturn(Optional.of(owner));
        given(studyRepository.save(any(Study.class))).willReturn(study);
        given(studyMemberRepository.save(any(StudyMember.class))).willReturn(studyMember);

        // when
        Long studyId = studyService.create(userId, request);

        // then
        assertThat(studyId).isEqualTo(10L);
        verify(studyRepository).save(any(Study.class));
    }

    @Test
    void 스터디_생성시_유저가_없으면_예외가_발생한다() {
        // given
        Long userId = 1L;

        StudyCreateRequest request = new StudyCreateRequest(
                "스프링 스터디",
                "스프링 공부",
                StudyVisibility.PUBLIC,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyService.create(userId, request))
                .isInstanceOf(RuntimeException.class);

        verify(studyRepository, never()).save(any());
    }

    @Test
    void 스터디_생성시_시작일자가_종료일자_이후면_예외가_발생한다() {
        // given
        Long userId = 1L;
        User owner = UserFixture.user(userId);

        StudyCreateRequest request = new StudyCreateRequest(
                "스프링 스터디",
                "스프링 공부",
                StudyVisibility.PUBLIC,
                LocalDate.now().plusDays(30),
                LocalDate.now()
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> studyService.create(userId, request))
                .isInstanceOf(InvalidStudyPeriodException.class);

        verify(studyRepository, never()).save(any());
    }

    @Test
    void 스터디_목록을_조회한다() {
        // given
        Long studyId = 1L;
        User owner = UserFixture.user(1L);
        Study study = StudyFixture.study(studyId, owner);
        List<Study> studies = List.of(study);

        given(studyRepository.findVisibleStudiesWithOwner())
                .willReturn(studies);

        // when
        List<StudySummaryResponse> responses = studyService.getStudies();

        // then
        assertThat(responses).hasSize(studies.size());

        StudySummaryResponse response = responses.get(0);

        assertThat(response.id()).isEqualTo(studyId);
        assertThat(response.title()).isEqualTo(study.getTitle());

        verify(studyRepository).findVisibleStudiesWithOwner();
    }

    @Test
    void 스터디를_상세_조회한다() {
        // given
        Long studyId = 1L;
        User owner = UserFixture.user(1L);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findVisibleStudyByIdWithOwnerAndCourse(studyId))
                .willReturn(Optional.of(study));

        // when
        StudyDetailResponse response = studyService.findStudy(studyId);

        // then
        assertThat(response.id()).isEqualTo(studyId);
        assertThat(response.title()).isEqualTo("스프링 스터디");
    }

    @Test
    void 스터디가_없으면_상세_조회에_실패한다() {
        // given
        Long studyId = 1L;

        given(studyRepository.findVisibleStudyByIdWithOwnerAndCourse(studyId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyService.findStudy(studyId))
                .isInstanceOf(StudyNotFoundException.class);
    }

    @Test
    void owner는_스터디를_수정할_수_있다() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User owner = UserFixture.user(userId);

        Study study = StudyFixture.study(studyId, owner);

        StudyUpdateRequest request = new StudyUpdateRequest(
                "새 제목",
                null,
                null,
                null,
                null
        );

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when
        studyService.updateStudyInfo(studyId, userId, request);

        // then
        assertThat(study.getTitle()).isEqualTo(request.title());
    }

    @Test
    void owner는_스터디_공개범위를_수정할_수_있다() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User owner = UserFixture.user(userId);

        Study study = StudyFixture.study(studyId, owner);

        StudyVisibility studyVisibility = StudyVisibility.PRIVATE;

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when
        studyService.updateStudyVisibility(studyId, userId, studyVisibility);

        // then
        assertThat(study.getVisibility()).isEqualTo(studyVisibility);
    }

    @Test
    void owner는_스터디_모집상태를_수정할_수_있다() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User owner = UserFixture.user(userId);

        Study study = StudyFixture.study(studyId, owner);

        StudyRecruitmentStatus recruitmentStatus = StudyRecruitmentStatus.CLOSED;

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when
        studyService.updateStudyRecruitment(studyId, userId, recruitmentStatus);

        // then
        assertThat(study.getRecruitmentStatus()).isEqualTo(recruitmentStatus);
    }

    @Test
    void owner는_스터디를_시작할_수_있다() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User owner = UserFixture.user(userId);

        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when
        studyService.startStudy(studyId, userId);

        // then
        assertThat(study.getStatus()).isEqualTo(StudyStatus.IN_PROGRESS);
    }

    @Test
    void owner는_스터디를_종료할_수_있다() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User owner = UserFixture.user(userId);

        Study study = StudyFixture.inprogressStudy(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when
        studyService.endStudy(studyId, userId);

        // then
        assertThat(study.getStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    void owner는_스터디를_삭제할_수_있다() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User owner = UserFixture.user(userId);

        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when
        studyService.deleteStudy(studyId, userId);

        // then
        assertThat(study.isDeleted()).isTrue();
    }

    @Test
    void owner가_아니면_스터디를_삭제할_수_없다() {
        // given
        Long ownerId = 1L;
        Long requestUserId = 2L;
        Long studyId = 10L;

        User owner = UserFixture.user(ownerId);

        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId))
                .willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.deleteStudy(studyId, requestUserId))
                .isInstanceOf(StudyAccessDeniedException.class);
    }
}
