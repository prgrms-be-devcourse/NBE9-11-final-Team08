package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lastwatchedlecture.service.LastWatchedLectureService;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.service.LectureProgressService;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class LectureServiceTest {

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private LectureProgressService lectureProgressService;

    @Mock
    private LastWatchedLectureService lastWatchedLectureService;

    @InjectMocks
    private LectureService lectureService;

    @Test
    void 강의를_성공적으로_생성하고_ID를_반환한다() {
        Long courseId = 10L;
        Long chapterId = 1L;
        LectureCreateRequest request = new LectureCreateRequest(
                "스프링 시큐리티 구조와 흐름",
                "videos/security.m3u8",
                UUID.randomUUID().toString(),
                "시큐리티 필터 체인 분석",
                1200,
                1,
                false
        );

        Course course = TestEntityFactory.course(courseId);
        ReflectionTestUtils.setField(course, "id", courseId);

        Chapter chapter = ChapterFixture.chapter(chapterId, "보안 기본", 1, course);
        Lecture savedLecture = LectureFixture.lecture(50L, request.title(), request.m3u8Path(), request.durationSeconds(), request.orderNo(), chapter);

        given(chapterRepository.findByIdWithCourse(chapterId)).willReturn(Optional.of(chapter));
        given(lectureRepository.save(any(Lecture.class))).willReturn(savedLecture);

        Long lectureId = lectureService.createLecture(courseId, chapterId, request);

        assertThat(lectureId).isEqualTo(50L);
        verify(chapterRepository).findByIdWithCourse(chapterId);
        verify(lectureRepository).save(any(Lecture.class));
    }

    @Test
    void 존재하지_않는_챕터_ID로_강의_생성_요청_시_예외가_발생한다() {
        Long courseId = 10L;
        Long invalidChapterId = 999L;
        LectureCreateRequest request = new LectureCreateRequest(
                "스프링 시큐리티 구조와 흐름",
                "videos/security.m3u8",
                UUID.randomUUID().toString(),
                "시큐리티 필터 체인 분석",
                1200,
                1,
                false
        );

        given(chapterRepository.findByIdWithCourse(invalidChapterId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureService.createLecture(courseId, invalidChapterId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.CHAPTER_NOT_FOUND.getMessage());

        verify(chapterRepository).findByIdWithCourse(invalidChapterId);
    }

    @Test
    void 요청된_코스_ID와_챕터의_코스_ID가_일치하지_않으면_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long chapterId = 1L;
        LectureCreateRequest request = new LectureCreateRequest(
                "스프링 시큐리티 구조와 흐름",
                "videos/security.m3u8",
                UUID.randomUUID().toString(),
                "시큐리티 필터 체인 분석",
                1200,
                1,
                false
        );

        Course realCourse = TestEntityFactory.course(10L);
        ReflectionTestUtils.setField(realCourse, "id", 10L);
        Chapter chapter = ChapterFixture.chapter(chapterId, "보안 기본", 1, realCourse);

        given(chapterRepository.findByIdWithCourse(chapterId)).willReturn(Optional.of(chapter));

        assertThatThrownBy(() -> lectureService.createLecture(invalidCourseId, chapterId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(chapterRepository).findByIdWithCourse(chapterId);
    }

    // ── 강의 입장 (enterLecture) ──────────────────────────────────────────

    @Test
    void 진행행이_생기는_정상_시청자는_입장하고_마지막시청을_기록한다() {
        Long courseId = 10L;
        Long lectureId = 50L;
        Long userId = 1L;
        Lecture lecture = lectureWithCourse(lectureId, courseId);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        // 수강권/무료맛보기면 ensureStarted 가 progress 행을 반환한다.
        given(lectureProgressService.ensureStarted(eq(userId), eq(lecture), any()))
                .willReturn(LectureProgress.start(userId, lectureId, 0, LocalDateTime.now()));

        LectureEnterResponse response = lectureService.enterLecture(courseId, CHAPTER_ID, lectureId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        verify(lastWatchedLectureService).record(userId, courseId, lectureId);
    }

    @Test
    void 미등록자는_입장은_되지만_마지막시청을_기록하지_않는다() {
        Long courseId = 10L;
        Long lectureId = 50L;
        Long userId = 1L;
        Lecture lecture = lectureWithCourse(lectureId, courseId);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        // 미등록·비무료면 ensureStarted 가 null 을 반환한다(입장 자체는 막지 않음 — 메타데이터 제공).
        given(lectureProgressService.ensureStarted(eq(userId), eq(lecture), any())).willReturn(null);

        LectureEnterResponse response = lectureService.enterLecture(courseId, CHAPTER_ID, lectureId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        // progress 없는 미등록자에게는 last_watched 행을 만들지 않는다(오염 방지).
        verify(lastWatchedLectureService, never()).record(any(), any(), any());
    }

    @Test
    void 존재하지_않는_강의_입장_시_예외가_발생한다() {
        given(lectureRepository.findByIdWithChapterAndCourse(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureService.enterLecture(10L, CHAPTER_ID, 999L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    void path_챕터ID가_강의의_챕터와_다르면_CHAPTER_NOT_FOUND() {
        Long courseId = 10L;
        Long lectureId = 50L;
        Lecture lecture = lectureWithCourse(lectureId, courseId);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));

        // 같은 강좌의 다른 챕터(999) 로 입장 시도
        assertThatThrownBy(() -> lectureService.enterLecture(courseId, 999L, lectureId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);

        verify(lastWatchedLectureService, never()).record(any(), any(), any());
    }

    @Test
    void path_코스ID가_강의의_코스와_다르면_COURSE_NOT_FOUND() {
        Long courseId = 10L;
        Long lectureId = 50L;
        Lecture lecture = lectureWithCourse(lectureId, courseId);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));

        // 챕터는 맞지만 다른 강좌(999) 로 입장 시도
        assertThatThrownBy(() -> lectureService.enterLecture(999L, CHAPTER_ID, lectureId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(lastWatchedLectureService, never()).record(any(), any(), any());
    }

    private static final Long CHAPTER_ID = 1L;

    private Lecture lectureWithCourse(Long lectureId, Long courseId) {
        Course course = TestEntityFactory.course(courseId);
        ReflectionTestUtils.setField(course, "id", courseId);
        Chapter chapter = ChapterFixture.chapter(CHAPTER_ID, "보안 기본", 1, course);
        return LectureFixture.lecture(lectureId, "강의1", "videos/1.m3u8", 600, 1, chapter);
    }
}