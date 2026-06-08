package com.team08.backend.domain.study.service;

import com.team08.backend.domain.comment.entity.Comment;
import com.team08.backend.domain.comment.repository.CommentRepository;
import com.team08.backend.domain.post.entity.Post;
import com.team08.backend.domain.post.repository.PostRepository;
import com.team08.backend.domain.study.dto.request.StudyApplicationCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCommentCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCommentUpdateRequest;
import com.team08.backend.domain.study.dto.request.StudyCreateRequest;
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
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

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
    public List<StudySummaryResponse> getStudies(Long userId) {
        List<Study> studies = studyRepository.findVisibleStudiesWithOwner(userId);
        return studies.stream()
                .map(StudySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyDetailResponse findStudy(Long id, Long userId) {
        Study study = studyRepository.findVisibleStudyByIdWithOwnerAndCourse(id, userId)
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

        studyMemberRepository.findByStudyIdAndUserId(id, applicantId)
                .ifPresentOrElse(
                        StudyMember::rejoinAsMember,
                        () -> {
                            StudyMember studyMember = StudyMember.createMember(application.getUser(), application.getStudy());
                            studyMemberRepository.save(studyMember);
                        }
                );

        log.debug("스터디 참여 신청 승인 studyId={}, applicationId={}, ownerId={}", id, applicationId, userId);
    }

    @Transactional
    public void rejectStudyApplication(Long id, Long applicationId, Long userId) {
        getOwnedStudy(id, userId);

        StudyApplication application = getStudyApplication(id, applicationId);
        application.reject();

        log.debug("스터디 참여 신청 거절 studyId={}, applicationId={}, ownerId={}", id, applicationId, userId);
    }

    @Transactional(readOnly = true)
    public List<StudyMemberResponse> getStudyMembers(Long id, Long userId) {
        getOwnedStudy(id, userId);

        return studyMemberRepository.findByStudyIdAndStatusOrderByJoinedAtAsc(id, StudyMemberStatus.ACTIVE)
                .stream()
                .map(StudyMemberResponse::from)
                .toList();
    }

    @Transactional
    public void kickStudyMember(Long id, Long memberId, Long userId) {
        getOwnedStudy(id, userId);

        StudyMember member = getStudyMember(id, memberId);
        member.kick();

        log.debug("스터디 멤버 강퇴 studyId={}, memberId={}, ownerId={}", id, memberId, userId);
    }

    @Transactional
    public void leaveStudy(Long id, Long userId) {
        StudyMember member = studyMemberRepository.findByStudyIdAndUserId(id, userId)
                .orElseThrow(StudyMemberNotFoundException::new);

        member.leave();

        log.debug("스터디 탈퇴 studyId={}, userId={}, memberId={}", id, userId, member.getId());
    }

    @Transactional
    public StudyPostDetailResponse createStudyPost(Long id, Long userId, StudyPostCreateRequest request) {
        StudyMember member = getActiveStudyMember(id, userId);
        Study study = getActiveStudyWithOwner(id);

        Post post = Post.create(
                study,
                member.getUser(),
                request.title(),
                request.content(),
                request.type()
        );
        Post savedPost = postRepository.save(post);

        log.debug("스터디 게시글 작성 studyId={}, postId={}, userId={}", id, savedPost.getId(), userId);

        return StudyPostDetailResponse.from(savedPost, List.of());
    }

    @Transactional(readOnly = true)
    public List<StudyPostSummaryResponse> getStudyPosts(Long id, Long userId) {
        getActiveStudyMember(id, userId);

        return postRepository.findByStudyIdAndDeletedAtIsNullOrderByCreatedAtDesc(id)
                .stream()
                .map(StudyPostSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyPostDetailResponse getStudyPost(Long id, Long postId, Long userId) {
        getActiveStudyMember(id, userId);

        Post post = getActiveStudyPost(id, postId);
        List<StudyCommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(StudyCommentResponse::from)
                .toList();

        return StudyPostDetailResponse.from(post, comments);
    }

    @Transactional
    public void updateStudyPost(Long id, Long postId, Long userId, StudyPostUpdateRequest request) {
        getActiveStudyMember(id, userId);
        Post post = getActiveStudyPost(id, postId);

        post.update(
                userId,
                request.title(),
                request.content(),
                request.type()
        );

        log.debug("스터디 게시글 수정 studyId={}, postId={}, userId={}", id, postId, userId);
    }

    @Transactional
    public void deleteStudyPost(Long id, Long postId, Long userId) {
        getActiveStudyMember(id, userId);
        Post post = getActiveStudyPost(id, postId);

        post.delete(userId);

        log.debug("스터디 게시글 삭제 studyId={}, postId={}, userId={}", id, postId, userId);
    }

    @Transactional
    public StudyCommentResponse createStudyComment(Long id, Long postId, Long userId, StudyCommentCreateRequest request) {
        StudyMember member = getActiveStudyMember(id, userId);
        Post post = getActiveStudyPost(id, postId);

        Comment comment = Comment.create(post, member.getUser(), request.content());
        Comment savedComment = commentRepository.save(comment);

        log.debug("스터디 댓글 작성 studyId={}, postId={}, commentId={}, userId={}", id, postId, savedComment.getId(), userId);

        return StudyCommentResponse.from(savedComment);
    }

    @Transactional
    public void updateStudyComment(Long id, Long postId, Long commentId, Long userId, StudyCommentUpdateRequest request) {
        getActiveStudyMember(id, userId);
        getActiveStudyPost(id, postId);

        Comment comment = getStudyComment(postId, commentId);
        comment.update(userId, request.content());

        log.debug("스터디 댓글 수정 studyId={}, postId={}, commentId={}, userId={}", id, postId, commentId, userId);
    }

    @Transactional
    public void deleteStudyComment(Long id, Long postId, Long commentId, Long userId) {
        getActiveStudyMember(id, userId);
        getActiveStudyPost(id, postId);

        Comment comment = getStudyComment(postId, commentId);
        comment.delete(userId);

        log.debug("스터디 댓글 삭제 studyId={}, postId={}, commentId={}, userId={}", id, postId, commentId, userId);
    }

    private Study getOwnedStudy(Long id, Long userId) {
        Study study = getActiveStudyWithOwner(id);

        study.validateOwner(userId);

        return study;
    }

    private Study getActiveStudyWithOwner(Long id) {
        return studyRepository.findActiveStudyByIdWithOwner(id)
                .orElseThrow(StudyNotFoundException::new);
    }

    private void validateCanApply(Long id, Long userId, Study study) {
        study.validateCanReceiveApplicationFrom(userId);

        if (studyMemberRepository.existsByStudyIdAndUserIdAndStatus(id, userId, StudyMemberStatus.KICKED)) {
            throw new StudyKickedMemberCannotApplyException();
        }

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

    private StudyMember getStudyMember(Long id, Long memberId) {
        return studyMemberRepository.findByIdAndStudyId(memberId, id)
                .orElseThrow(StudyMemberNotFoundException::new);
    }

    private StudyMember getActiveStudyMember(Long id, Long userId) {
        return studyMemberRepository.findByStudyIdAndUserIdAndStatus(id, userId, StudyMemberStatus.ACTIVE)
                .orElseThrow(StudyAccessDeniedException::new);
    }

    private Post getActiveStudyPost(Long id, Long postId) {
        return postRepository.findByIdAndStudyIdAndDeletedAtIsNull(postId, id)
                .orElseThrow(StudyPostNotFoundException::new);
    }

    private Comment getStudyComment(Long postId, Long commentId) {
        return commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(StudyCommentNotFoundException::new);
    }

}
