package com.team08.backend.domain.study.service;

import com.team08.backend.domain.fixture.StudyFixture;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.comment.entity.Comment;
import com.team08.backend.domain.comment.repository.CommentRepository;
import com.team08.backend.domain.post.entity.Post;
import com.team08.backend.domain.post.entity.PostType;
import com.team08.backend.domain.post.repository.PostRepository;
import com.team08.backend.domain.study.dto.request.StudyApplicationCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCommentCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCommentUpdateRequest;
import com.team08.backend.domain.study.dto.request.StudyPostCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyPostUpdateRequest;
import com.team08.backend.domain.study.dto.request.StudyUpdateRequest;
import com.team08.backend.domain.study.dto.response.StudyApplicationResponse;
import com.team08.backend.domain.study.dto.response.StudyCommentResponse;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudyMemberResponse;
import com.team08.backend.domain.study.dto.response.StudyPostDetailResponse;
import com.team08.backend.domain.study.dto.response.StudyPostSummaryResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.*;
import com.team08.backend.domain.study.exception.*;
import com.team08.backend.domain.study.repository.StudyApplicationRepository;
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
import static org.mockito.Mockito.times;
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

    @Mock
    private StudyApplicationRepository studyApplicationRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

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
        Long ownerId = 1L;
        Long studyId = 1L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);
        List<Study> studies = List.of(study);

        given(studyRepository.findVisibleStudiesWithOwner(ownerId))
                .willReturn(studies);

        // when
        List<StudySummaryResponse> responses = studyService.getStudies(ownerId);

        // then
        assertThat(responses).hasSize(studies.size());

        StudySummaryResponse response = responses.get(0);

        assertThat(response.id()).isEqualTo(studyId);
        assertThat(response.title()).isEqualTo(study.getTitle());

        verify(studyRepository).findVisibleStudiesWithOwner(ownerId);
    }

    @Test
    void 스터디를_상세_조회한다() {
        // given
        Long ownerId = 1L;
        Long studyId = 1L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findVisibleStudyByIdWithOwnerAndCourse(studyId, ownerId))
                .willReturn(Optional.of(study));

        // when
        StudyDetailResponse response = studyService.findStudy(studyId, ownerId);

        // then
        assertThat(response.id()).isEqualTo(studyId);
        assertThat(response.title()).isEqualTo("스프링 스터디");
    }

    @Test
    void 스터디가_없으면_상세_조회에_실패한다() {
        // given
        Long studyId = 1L;

        given(studyRepository.findVisibleStudyByIdWithOwnerAndCourse(studyId, null))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyService.findStudy(studyId, null))
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

    @Test
    void 모집중인_스터디에_참여_신청한다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyApplicationCreateRequest request = new StudyApplicationCreateRequest("참여하고 싶습니다.");
        StudyApplication application = StudyApplication.create(study, user, request.message());
        ReflectionTestUtils.setField(application, "id", 100L);

        given(studyRepository.findActiveStudyById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.KICKED))
                .willReturn(false);
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(false);
        given(studyApplicationRepository.existsByStudyIdAndUserId(studyId, userId)).willReturn(false);
        given(studyApplicationRepository.save(any(StudyApplication.class))).willReturn(application);

        // when
        StudyApplicationResponse response = studyService.applyStudy(studyId, userId, request);

        // then
        assertThat(response.applicationId()).isEqualTo(100L);
        assertThat(response.studyId()).isEqualTo(studyId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.message()).isEqualTo(request.message());
        assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        verify(studyApplicationRepository).save(any(StudyApplication.class));
    }

    @Test
    void 모집이_종료된_스터디에는_참여_신청할_수_없다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        Study study = StudyFixture.study(studyId, owner);
        study.changeRecruitmentStatus(StudyRecruitmentStatus.CLOSED);

        given(studyRepository.findActiveStudyById(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.applyStudy(
                studyId,
                userId,
                new StudyApplicationCreateRequest("참여하고 싶습니다.")
        )).isInstanceOf(StudyRecruitmentClosedException.class);

        verify(studyApplicationRepository, never()).save(any());
    }

    @Test
    void 이미_스터디_멤버인_사용자는_참여_신청할_수_없다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.KICKED))
                .willReturn(false);
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyService.applyStudy(
                studyId,
                userId,
                new StudyApplicationCreateRequest("참여하고 싶습니다.")
        )).isInstanceOf(StudyAlreadyMemberException.class);

        verify(studyApplicationRepository, never()).save(any());
    }

    @Test
    void 동일_사용자는_동일_스터디에_중복_신청할_수_없다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.KICKED))
                .willReturn(false);
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(false);
        given(studyApplicationRepository.existsByStudyIdAndUserId(studyId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyService.applyStudy(
                studyId,
                userId,
                new StudyApplicationCreateRequest("참여하고 싶습니다.")
        )).isInstanceOf(DuplicateStudyApplicationException.class);

        verify(studyApplicationRepository, never()).save(any());
    }

    @Test
    void 사용자는_자신의_참여_신청을_취소할_수_있다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyApplication application = StudyApplication.create(study, user, "참여하고 싶습니다.");

        given(studyApplicationRepository.findByStudyIdAndUserId(studyId, userId))
                .willReturn(Optional.of(application));

        // when
        studyService.cancelStudyApplication(studyId, userId);

        // then
        verify(studyApplicationRepository).delete(application);
    }

    @Test
    void 스터디_생성자는_참여_신청_목록을_조회할_수_있다() {
        // given
        Long ownerId = 1L;
        Long studyId = 10L;
        User owner = UserFixture.user(ownerId);
        User user = UserFixture.user(2L);
        Study study = StudyFixture.study(studyId, owner);
        StudyApplication application = StudyApplication.create(study, user, "참여하고 싶습니다.");
        ReflectionTestUtils.setField(application, "id", 100L);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyApplicationRepository.findByStudyIdOrderByAppliedAtAsc(studyId))
                .willReturn(List.of(application));

        // when
        List<StudyApplicationResponse> responses = studyService.getStudyApplications(studyId, ownerId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).applicationId()).isEqualTo(100L);
        assertThat(responses.get(0).status()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void 스터디_생성자가_참여_신청을_승인하면_멤버로_등록된다() {
        // given
        Long ownerId = 1L;
        Long studyId = 10L;
        Long applicationId = 100L;
        User owner = UserFixture.user(ownerId);
        User applicant = UserFixture.user(2L);
        Study study = StudyFixture.study(studyId, owner);
        StudyApplication application = StudyApplication.create(study, applicant, "참여하고 싶습니다.");

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyApplicationRepository.findByIdAndStudyId(applicationId, studyId))
                .willReturn(Optional.of(application));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, applicant.getId(), StudyMemberStatus.ACTIVE))
                .willReturn(false);

        // when
        studyService.approveStudyApplication(studyId, applicationId, ownerId);

        // then
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        verify(studyMemberRepository, times(1)).save(any(StudyMember.class));
    }

    @Test
    void 스터디_생성자가_참여_신청을_거절하면_멤버로_등록하지_않는다() {
        // given
        Long ownerId = 1L;
        Long studyId = 10L;
        Long applicationId = 100L;
        User owner = UserFixture.user(ownerId);
        User applicant = UserFixture.user(2L);
        Study study = StudyFixture.study(studyId, owner);
        StudyApplication application = StudyApplication.create(study, applicant, "참여하고 싶습니다.");

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyApplicationRepository.findByIdAndStudyId(applicationId, studyId))
                .willReturn(Optional.of(application));

        // when
        studyService.rejectStudyApplication(studyId, applicationId, ownerId);

        // then
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    void 생성자가_아니면_참여_신청_목록을_조회할_수_없다() {
        // given
        Long ownerId = 1L;
        Long requestUserId = 2L;
        Long studyId = 10L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.getStudyApplications(studyId, requestUserId))
                .isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 생성자가_아니면_참여_신청을_승인할_수_없다() {
        // given
        Long ownerId = 1L;
        Long requestUserId = 2L;
        Long studyId = 10L;
        Long applicationId = 100L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.approveStudyApplication(studyId, applicationId, requestUserId))
                .isInstanceOf(StudyAccessDeniedException.class);

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    void 생성자가_아니면_참여_신청을_거절할_수_없다() {
        // given
        Long ownerId = 1L;
        Long requestUserId = 2L;
        Long studyId = 10L;
        Long applicationId = 100L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.rejectStudyApplication(studyId, applicationId, requestUserId))
                .isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 스터디_생성자는_멤버_목록을_조회할_수_있다() {
        // given
        Long ownerId = 1L;
        Long studyId = 10L;
        User owner = UserFixture.user(ownerId);
        User user = UserFixture.user(2L);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember ownerMember = StudyMember.createOwner(owner, study);
        StudyMember member = StudyMember.createMember(user, study);
        ReflectionTestUtils.setField(ownerMember, "id", 100L);
        ReflectionTestUtils.setField(member, "id", 101L);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndStatusOrderByJoinedAtAsc(studyId, StudyMemberStatus.ACTIVE))
                .willReturn(List.of(ownerMember, member));

        // when
        List<StudyMemberResponse> responses = studyService.getStudyMembers(studyId, ownerId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).role()).isEqualTo(StudyMemberRole.OWNER);
        assertThat(responses.get(1).role()).isEqualTo(StudyMemberRole.MEMBER);
    }

    @Test
    void 생성자가_아니면_멤버_목록을_조회할_수_없다() {
        // given
        Long ownerId = 1L;
        Long requestUserId = 2L;
        Long studyId = 10L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.getStudyMembers(studyId, requestUserId))
                .isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 스터디_생성자는_멤버를_강퇴할_수_있다() {
        // given
        Long ownerId = 1L;
        Long studyId = 10L;
        Long memberId = 100L;
        User owner = UserFixture.user(ownerId);
        User user = UserFixture.user(2L);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByIdAndStudyId(memberId, studyId)).willReturn(Optional.of(member));

        // when
        studyService.kickStudyMember(studyId, memberId, ownerId);

        // then
        assertThat(member.getStatus()).isEqualTo(StudyMemberStatus.KICKED);
        assertThat(member.getKickedAt()).isNotNull();
    }

    @Test
    void 스터디_생성자는_강퇴할_수_없다() {
        // given
        Long ownerId = 1L;
        Long studyId = 10L;
        Long ownerMemberId = 100L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember ownerMember = StudyMember.createOwner(owner, study);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByIdAndStudyId(ownerMemberId, studyId)).willReturn(Optional.of(ownerMember));

        // when & then
        assertThatThrownBy(() -> studyService.kickStudyMember(studyId, ownerMemberId, ownerId))
                .isInstanceOf(StudyOwnerCannotBeKickedException.class);
    }

    @Test
    void 생성자가_아니면_멤버를_강퇴할_수_없다() {
        // given
        Long ownerId = 1L;
        Long requestUserId = 2L;
        Long studyId = 10L;
        Long memberId = 100L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyService.kickStudyMember(studyId, memberId, requestUserId))
                .isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 스터디_멤버는_자발적으로_탈퇴할_수_있다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);

        given(studyMemberRepository.findByStudyIdAndUserId(studyId, userId)).willReturn(Optional.of(member));

        // when
        studyService.leaveStudy(studyId, userId);

        // then
        assertThat(member.getStatus()).isEqualTo(StudyMemberStatus.LEFT);
        assertThat(member.getLeftAt()).isNotNull();
    }

    @Test
    void 스터디_생성자는_탈퇴할_수_없다() {
        // given
        Long studyId = 10L;
        Long ownerId = 1L;
        User owner = UserFixture.user(ownerId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember ownerMember = StudyMember.createOwner(owner, study);

        given(studyMemberRepository.findByStudyIdAndUserId(studyId, ownerId)).willReturn(Optional.of(ownerMember));

        // when & then
        assertThatThrownBy(() -> studyService.leaveStudy(studyId, ownerId))
                .isInstanceOf(StudyOwnerCannotLeaveException.class);
    }

    @Test
    void 강퇴된_사용자는_재신청할_수_없다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        Study study = StudyFixture.study(studyId, owner);

        given(studyRepository.findActiveStudyById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.KICKED))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyService.applyStudy(
                studyId,
                userId,
                new StudyApplicationCreateRequest("다시 참여하고 싶습니다.")
        )).isInstanceOf(StudyKickedMemberCannotApplyException.class);

        verify(studyApplicationRepository, never()).save(any());
    }

    @Test
    void 활성_스터디_멤버는_게시글을_작성할_수_있다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        StudyPostCreateRequest request = new StudyPostCreateRequest("질문", "내용", PostType.FREE);
        Post post = Post.create(study, user, request.title(), request.content(), request.type());
        ReflectionTestUtils.setField(post, "id", 100L);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        StudyPostDetailResponse response = studyService.createStudyPost(studyId, userId, request);

        // then
        assertThat(response.postId()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.type()).isEqualTo(PostType.FREE);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void 비멤버는_게시글을_작성할_수_없다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyService.createStudyPost(
                studyId,
                userId,
                new StudyPostCreateRequest("질문", "내용", PostType.FREE)
        )).isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 스터디_생성자만_NOTICE_게시글을_작성할_수_있다() {
        // given
        Long studyId = 10L;
        Long memberId = 2L;
        User owner = UserFixture.user(1L);
        User memberUser = UserFixture.user(memberId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(memberUser, study);

        given(studyRepository.findActiveStudyByIdWithOwner(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, memberId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> studyService.createStudyPost(
                studyId,
                memberId,
                new StudyPostCreateRequest("공지", "내용", PostType.NOTICE)
        )).isInstanceOf(StudyAccessDeniedException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void 게시글_목록은_삭제되지_않은_게시글만_조회한다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        Post post = Post.create(study, user, "질문", "내용", PostType.FREE);
        ReflectionTestUtils.setField(post, "id", 100L);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByStudyIdAndDeletedAtIsNullOrderByCreatedAtDesc(studyId))
                .willReturn(List.of(post));

        // when
        List<StudyPostSummaryResponse> responses = studyService.getStudyPosts(studyId, userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).postId()).isEqualTo(100L);
    }

    @Test
    void 비멤버는_게시글_목록을_조회할_수_없다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyService.getStudyPosts(studyId, userId))
                .isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 게시글_상세는_댓글_목록을_포함한다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        Long postId = 100L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        Post post = Post.create(study, user, "질문", "내용", PostType.FREE);
        Comment comment = Comment.create(post, user, "댓글");
        ReflectionTestUtils.setField(post, "id", postId);
        ReflectionTestUtils.setField(comment, "id", 200L);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(postId))
                .willReturn(List.of(comment));

        // when
        StudyPostDetailResponse response = studyService.getStudyPost(studyId, postId, userId);

        // then
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).content()).isEqualTo("댓글");
    }

    @Test
    void 게시글_작성자는_자신의_게시글을_수정할_수_있다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        Long postId = 100L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        Post post = Post.create(study, user, "질문", "내용", PostType.FREE);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));

        // when
        studyService.updateStudyPost(studyId, postId, userId, new StudyPostUpdateRequest("수정", "수정 내용", PostType.FREE));

        // then
        assertThat(post.getTitle()).isEqualTo("수정");
        assertThat(post.getContent()).isEqualTo("수정 내용");
    }

    @Test
    void 작성자가_아니면_게시글을_수정할_수_없다() {
        // given
        Long studyId = 10L;
        Long writerId = 2L;
        Long requestUserId = 3L;
        Long postId = 100L;
        User owner = UserFixture.user(1L);
        User writer = UserFixture.user(writerId);
        User requestUser = UserFixture.user(requestUserId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(requestUser, study);
        Post post = Post.create(study, writer, "질문", "내용", PostType.FREE);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, requestUserId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> studyService.updateStudyPost(
                studyId,
                postId,
                requestUserId,
                new StudyPostUpdateRequest("수정", "수정 내용", PostType.FREE)
        )).isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 스터디_생성자는_타인의_게시글을_삭제할_수_있다() {
        // given
        Long ownerId = 1L;
        Long writerId = 2L;
        Long studyId = 10L;
        Long postId = 100L;
        User owner = UserFixture.user(ownerId);
        User writer = UserFixture.user(writerId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember ownerMember = StudyMember.createOwner(owner, study);
        Post post = Post.create(study, writer, "질문", "내용", PostType.FREE);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, ownerId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));

        // when
        studyService.deleteStudyPost(studyId, postId, ownerId);

        // then
        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void 활성_스터디_멤버는_댓글을_작성할_수_있다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        Long postId = 100L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        Post post = Post.create(study, owner, "질문", "내용", PostType.FREE);
        Comment comment = Comment.create(post, user, "댓글");
        ReflectionTestUtils.setField(comment, "id", 200L);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        // when
        StudyCommentResponse response = studyService.createStudyComment(
                studyId,
                postId,
                userId,
                new StudyCommentCreateRequest("댓글")
        );

        // then
        assertThat(response.commentId()).isEqualTo(200L);
        assertThat(response.content()).isEqualTo("댓글");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void 비멤버는_댓글을_작성할_수_없다() {
        // given
        Long studyId = 10L;
        Long postId = 100L;
        Long userId = 2L;

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyService.createStudyComment(
                studyId,
                postId,
                userId,
                new StudyCommentCreateRequest("댓글")
        )).isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 댓글_작성자는_자신의_댓글을_수정하고_삭제할_수_있다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        Long postId = 100L;
        Long commentId = 200L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        Post post = Post.create(study, owner, "질문", "내용", PostType.FREE);
        Comment comment = Comment.create(post, user, "댓글");

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));
        given(commentRepository.findByIdAndPostId(commentId, postId))
                .willReturn(Optional.of(comment));

        // when
        studyService.updateStudyComment(studyId, postId, commentId, userId, new StudyCommentUpdateRequest("수정 댓글"));
        studyService.deleteStudyComment(studyId, postId, commentId, userId);

        // then
        assertThat(comment.getContent()).isEqualTo("수정 댓글");
        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    void 작성자가_아니면_댓글을_수정할_수_없다() {
        // given
        Long studyId = 10L;
        Long writerId = 2L;
        Long requestUserId = 3L;
        Long postId = 100L;
        Long commentId = 200L;
        User owner = UserFixture.user(1L);
        User writer = UserFixture.user(writerId);
        User requestUser = UserFixture.user(requestUserId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(requestUser, study);
        Post post = Post.create(study, owner, "질문", "내용", PostType.FREE);
        Comment comment = Comment.create(post, writer, "댓글");

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, requestUserId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));
        given(commentRepository.findByIdAndPostId(commentId, postId))
                .willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> studyService.updateStudyComment(
                studyId,
                postId,
                commentId,
                requestUserId,
                new StudyCommentUpdateRequest("수정 댓글")
        )).isInstanceOf(StudyAccessDeniedException.class);
    }

    @Test
    void 스터디_생성자는_타인의_댓글을_삭제할_수_있다() {
        // given
        Long ownerId = 1L;
        Long writerId = 2L;
        Long studyId = 10L;
        Long postId = 100L;
        Long commentId = 200L;
        User owner = UserFixture.user(ownerId);
        User writer = UserFixture.user(writerId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember ownerMember = StudyMember.createOwner(owner, study);
        Post post = Post.create(study, owner, "질문", "내용", PostType.FREE);
        Comment comment = Comment.create(post, writer, "댓글");

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, ownerId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));
        given(commentRepository.findByIdAndPostId(commentId, postId))
                .willReturn(Optional.of(comment));

        // when
        studyService.deleteStudyComment(studyId, postId, commentId, ownerId);

        // then
        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    void 삭제된_댓글은_상세_응답에서_placeholder로_표시된다() {
        // given
        Long studyId = 10L;
        Long userId = 2L;
        Long postId = 100L;
        User owner = UserFixture.user(1L);
        User user = UserFixture.user(userId);
        Study study = StudyFixture.study(studyId, owner);
        StudyMember member = StudyMember.createMember(user, study);
        Post post = Post.create(study, user, "질문", "내용", PostType.FREE);
        Comment comment = Comment.create(post, user, "댓글");
        comment.delete(userId);

        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, studyId))
                .willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(postId))
                .willReturn(List.of(comment));

        // when
        StudyPostDetailResponse response = studyService.getStudyPost(studyId, postId, userId);

        // then
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).deleted()).isTrue();
        assertThat(response.comments().get(0).content()).isEqualTo("삭제된 댓글입니다.");
    }
}
