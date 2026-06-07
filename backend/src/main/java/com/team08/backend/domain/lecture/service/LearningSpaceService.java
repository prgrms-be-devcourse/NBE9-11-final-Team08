package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.lecture.dto.CommentCreateRequest;
import com.team08.backend.domain.lecture.dto.CommentResponse;
import com.team08.backend.domain.lecture.dto.LearningSpaceResponse;
import com.team08.backend.domain.lecture.dto.LectureSummaryResponse;
import com.team08.backend.domain.lecture.dto.ProgressResponse;
import com.team08.backend.domain.lecture.dto.ProgressUpdateRequest;
import com.team08.backend.domain.lecture.dto.ReflectionResponse;
import com.team08.backend.domain.lecture.dto.ReflectionUpsertRequest;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.entity.LectureComment;
import com.team08.backend.domain.lecture.entity.LectureProgress;
import com.team08.backend.domain.lecture.entity.LectureReflection;
import com.team08.backend.domain.lecture.repository.LectureCommentRepository;
import com.team08.backend.domain.lecture.repository.LectureProgressRepository;
import com.team08.backend.domain.lecture.repository.LectureReflectionRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.report.entity.DailyLectureStat;
import com.team08.backend.domain.report.repository.DailyLectureStatRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningSpaceService {

    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureReflectionRepository lectureReflectionRepository;
    private final LectureCommentRepository lectureCommentRepository;
    private final DailyLectureStatRepository dailyLectureStatRepository;
    private final UserRepository userRepository;

    @Transactional
    public LearningSpaceResponse enterChapter(Long userId, Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "챕터를 찾을 수 없습니다."));

        Lecture lecture = lectureProgressRepository.findFirstByUserIdAndLectureChapterIdOrderByUpdatedAtDesc(userId, chapterId)
                .map(LectureProgress::getLecture)
                .orElseGet(() -> lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "챕터에 강의가 없습니다.")));

        return getLearningSpace(userId, lecture.getId());
    }

    @Transactional
    public LearningSpaceResponse enterRecentLecture(Long userId) {
        Lecture lecture = lectureProgressRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId)
                .map(LectureProgress::getLecture)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "최근 수강한 강의가 없습니다."));

        return getLearningSpace(userId, lecture.getId());
    }

    @Transactional
    public LearningSpaceResponse getLearningSpace(Long userId, Long lectureId) {
        Lecture lecture = getLecture(lectureId);
        User user = getUser(userId);
        LectureProgress progress = getOrCreateProgress(user, lecture);
        LectureReflection reflection = lectureReflectionRepository.findByUserIdAndLectureId(userId, lectureId).orElse(null);

        return new LearningSpaceResponse(
                LectureSummaryResponse.from(lecture),
                ProgressResponse.from(progress),
                ReflectionResponse.from(reflection)
        );
    }

    @Transactional
    public ProgressResponse updateProgress(Long userId, Long lectureId, ProgressUpdateRequest request) {
        Lecture lecture = getLecture(lectureId);
        User user = getUser(userId);
        LectureProgress progress = getOrCreateProgress(user, lecture);

        boolean newlyCompleted = request.completed() != null && request.completed()
                ? progress.complete()
                : progress.updatePosition(request.positionSeconds());

        if (newlyCompleted) {
            increaseTodayCompletedLectureCount(user, lecture);
        }

        return ProgressResponse.from(progress);
    }

    public ReflectionResponse getReflection(Long userId, Long lectureId) {
        LectureReflection reflection = lectureReflectionRepository.findByUserIdAndLectureId(userId, lectureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));
        return ReflectionResponse.from(reflection);
    }

    @Transactional
    public ReflectionResponse upsertReflection(Long userId, Long lectureId, ReflectionUpsertRequest request) {
        Lecture lecture = getLecture(lectureId);
        User user = getUser(userId);

        LectureReflection reflection = lectureReflectionRepository.findByUserIdAndLectureId(userId, lectureId)
                .map(existing -> {
                    existing.update(request.content());
                    return existing;
                })
                .orElseGet(() -> lectureReflectionRepository.save(LectureReflection.create(lecture, user, request.content())));

        return ReflectionResponse.from(reflection);
    }

    public List<CommentResponse> getComments(Long lectureId, Long afterId) {
        getLecture(lectureId);
        List<LectureComment> comments = afterId == null
                ? lectureCommentRepository.findByLectureIdAndDeletedFalseOrderByCreatedAtAsc(lectureId)
                : lectureCommentRepository.findByLectureIdAndIdGreaterThanAndDeletedFalseOrderByCreatedAtAsc(lectureId, afterId);

        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long userId, Long lectureId, CommentCreateRequest request) {
        Lecture lecture = getLecture(lectureId);
        User user = getUser(userId);
        LectureComment parent = null;

        if (request.parentId() != null) {
            parent = lectureCommentRepository.findById(request.parentId())
                    .filter(comment -> comment.getLecture().getId().equals(lectureId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "부모 댓글이 올바르지 않습니다."));
        }

        LectureComment comment = lectureCommentRepository.save(
                LectureComment.create(lecture, user, parent, request.content(), request.timestampSeconds())
        );

        return CommentResponse.from(comment);
    }

    private LectureProgress getOrCreateProgress(User user, Lecture lecture) {
        return lectureProgressRepository.findByUserIdAndLectureId(user.getId(), lecture.getId())
                .orElseGet(() -> lectureProgressRepository.save(LectureProgress.start(lecture, user)));
    }

    private void increaseTodayCompletedLectureCount(User user, Lecture lecture) {
        LocalDate today = LocalDate.now();
        DailyLectureStat stat = dailyLectureStatRepository
                .findByUserIdAndCourseIdAndStatDate(user.getId(), lecture.getChapter().getCourse().getId(), today)
                .orElseGet(() -> dailyLectureStatRepository.save(
                        DailyLectureStat.create(user, lecture.getChapter().getCourse(), today)
                ));
        stat.increaseCompletedLectureCount();
    }

    private Lecture getLecture(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
