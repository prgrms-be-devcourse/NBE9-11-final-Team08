package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.lecture.dto.CommentCreateRequest;
import com.team08.backend.domain.lecture.dto.CommentResponse;
import com.team08.backend.domain.lecture.dto.LearningSpaceResponse;
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
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningSpaceService 단위 테스트")
class LearningSpaceServiceTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private LectureProgressRepository lectureProgressRepository;
    @Mock
    private LectureReflectionRepository lectureReflectionRepository;
    @Mock
    private LectureCommentRepository lectureCommentRepository;
    @Mock
    private DailyLectureStatRepository dailyLectureStatRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LearningSpaceService learningSpaceService;

    private User user;
    private Chapter chapter;
    private Lecture firstLecture;
    private Lecture secondLecture;

    @BeforeEach
    void setUp() {
        user = TestEntityFactory.user("test@example.com", "테스트유저");
        Course course = TestEntityFactory.course(user, "스프링 부트");
        chapter = TestEntityFactory.chapter(course, "스프링 기초", 1);
        firstLecture = TestEntityFactory.lecture(chapter, "Bean 기초", "v1", 600, 1);
        secondLecture = TestEntityFactory.lecture(chapter, "DI 심화", "v2", 600, 2);

        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(course, "id", 10L);
        ReflectionTestUtils.setField(chapter, "id", 1L);
        ReflectionTestUtils.setField(firstLecture, "id", 1L);
        ReflectionTestUtils.setField(secondLecture, "id", 2L);
    }

    @Nested
    @DisplayName("챕터 입장")
    class EnterChapter {

        @Test
        @DisplayName("처음 학습하는 챕터이면 첫 강의로 이동한다")
        void enterFirstLectureWhenNoProgress() {
            given(chapterRepository.findById(1L)).willReturn(Optional.of(chapter));
            given(lectureProgressRepository.findFirstByUserIdAndLectureChapterIdOrderByUpdatedAtDesc(1L, 1L))
                    .willReturn(Optional.empty());
            given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(1L)).willReturn(Optional.of(firstLecture));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(lectureProgressRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.empty());
            given(lectureProgressRepository.save(any(LectureProgress.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(lectureReflectionRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.empty());

            LearningSpaceResponse response = learningSpaceService.enterChapter(1L, 1L);

            assertThat(response.lecture().title()).isEqualTo("Bean 기초");
            assertThat(response.lecture().orderNo()).isEqualTo(1);
        }

        @Test
        @DisplayName("기존 학습 기록이 있으면 해당 챕터의 최근 학습 강의로 이동한다")
        void enterRecentLectureInChapter() {
            LectureProgress progress = LectureProgress.start(secondLecture, user);

            given(chapterRepository.findById(1L)).willReturn(Optional.of(chapter));
            given(lectureProgressRepository.findFirstByUserIdAndLectureChapterIdOrderByUpdatedAtDesc(1L, 1L))
                    .willReturn(Optional.of(progress));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureRepository.findById(2L)).willReturn(Optional.of(secondLecture));
            given(lectureProgressRepository.findByUserIdAndLectureId(1L, 2L)).willReturn(Optional.of(progress));
            given(lectureReflectionRepository.findByUserIdAndLectureId(1L, 2L)).willReturn(Optional.empty());

            LearningSpaceResponse response = learningSpaceService.enterChapter(1L, 1L);

            assertThat(response.lecture().title()).isEqualTo("DI 심화");
        }

        @Test
        @DisplayName("존재하지 않는 챕터는 404를 반환한다")
        void chapterNotFound() {
            given(chapterRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> learningSpaceService.enterChapter(1L, 99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("최근 수강 강의 입장")
    class EnterRecentLecture {

        @Test
        @DisplayName("최근 수강한 강의가 있으면 해당 강의 러닝스페이스로 이동한다")
        void enterRecent() {
            LectureProgress progress = LectureProgress.start(firstLecture, user);

            given(lectureProgressRepository.findFirstByUserIdOrderByUpdatedAtDesc(1L)).willReturn(Optional.of(progress));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(lectureProgressRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.of(progress));
            given(lectureReflectionRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.empty());

            LearningSpaceResponse response = learningSpaceService.enterRecentLecture(1L);

            assertThat(response.lecture().title()).isEqualTo("Bean 기초");
        }

        @Test
        @DisplayName("최근 수강한 강의가 없으면 404를 반환한다")
        void noRecentLecture() {
            given(lectureProgressRepository.findFirstByUserIdOrderByUpdatedAtDesc(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> learningSpaceService.enterRecentLecture(1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("진행률 갱신")
    class UpdateProgress {

        @Test
        @DisplayName("재생 위치 갱신 시 진행률이 반영된다")
        void updatePosition() {
            LectureProgress progress = LectureProgress.start(firstLecture, user);

            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureProgressRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.of(progress));

            ProgressResponse response = learningSpaceService.updateProgress(1L, 1L, new ProgressUpdateRequest(150, null));

            assertThat(response.lastPositionSeconds()).isEqualTo(150);
            assertThat(response.progressPercent()).isEqualTo(25);
            assertThat(response.completed()).isFalse();
        }

        @Test
        @DisplayName("신규 완료 시 일별 수강 통계가 증가한다")
        void increaseDailyStatOnCompletion() {
            Course course = TestEntityFactory.course(user, "코스");
            ReflectionTestUtils.setField(chapter, "course", course);
            ReflectionTestUtils.setField(firstLecture, "chapter", chapter);
            LectureProgress progress = LectureProgress.start(firstLecture, user);
            DailyLectureStat stat = DailyLectureStat.create(user, course, java.time.LocalDate.now());

            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureProgressRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.of(progress));
            given(dailyLectureStatRepository.findByUserIdAndCourseIdAndStatDate(any(), any(), any()))
                    .willReturn(Optional.of(stat));

            learningSpaceService.updateProgress(1L, 1L, new ProgressUpdateRequest(0, true));

            assertThat(stat.getCompletedLectureCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("회고")
    class Reflection {

        @Test
        @DisplayName("회고는 사용자와 강의당 하나만 작성된다")
        void upsertReflectionCreatesOnce() {
            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureReflectionRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.empty());
            given(lectureReflectionRepository.save(any(LectureReflection.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ReflectionResponse created = learningSpaceService.upsertReflection(
                    1L, 1L, new ReflectionUpsertRequest("첫 회고")
            );

            assertThat(created.content()).isEqualTo("첫 회고");
        }

        @Test
        @DisplayName("기존 회고가 있으면 수정한다")
        void upsertReflectionUpdatesExisting() {
            LectureReflection existing = LectureReflection.create(firstLecture, user, "기존 회고");

            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lectureReflectionRepository.findByUserIdAndLectureId(1L, 1L)).willReturn(Optional.of(existing));

            ReflectionResponse updated = learningSpaceService.upsertReflection(
                    1L, 1L, new ReflectionUpsertRequest("수정된 회고")
            );

            assertThat(updated.content()).isEqualTo("수정된 회고");
            verify(lectureReflectionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("댓글")
    class Comments {

        @Test
        @DisplayName("댓글 목록을 조회할 수 있다")
        void getComments() {
            LectureComment comment = LectureComment.create(firstLecture, user, null, "좋은 강의", 120);

            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(lectureCommentRepository.findByLectureIdAndDeletedFalseOrderByCreatedAtAsc(1L))
                    .willReturn(List.of(comment));

            List<CommentResponse> responses = learningSpaceService.getComments(1L, null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).content()).isEqualTo("좋은 강의");
        }

        @Test
        @DisplayName("afterId 이후 댓글만 조회해 새로고침에 사용할 수 있다")
        void getCommentsAfterId() {
            LectureComment newComment = LectureComment.create(firstLecture, user, null, "새 댓글", null);

            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(lectureCommentRepository.findByLectureIdAndIdGreaterThanAndDeletedFalseOrderByCreatedAtAsc(1L, 5L))
                    .willReturn(List.of(newComment));

            List<CommentResponse> responses = learningSpaceService.getComments(1L, 5L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).content()).isEqualTo("새 댓글");
        }

        @Test
        @DisplayName("단댓글을 작성할 수 있다")
        void createComment() {
            given(lectureRepository.findById(1L)).willReturn(Optional.of(firstLecture));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            when(lectureCommentRepository.save(any(LectureComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CommentResponse response = learningSpaceService.createComment(
                    1L, 1L, new CommentCreateRequest("댓글 내용", null, 60)
            );

            assertThat(response.content()).isEqualTo("댓글 내용");
            assertThat(response.timestampSeconds()).isEqualTo(60);
        }
    }
}
