package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.enrollment.service.EnrollmentAccessValidator;
import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lastwatchedlecture.service.LastWatchedLectureService;
import com.team08.backend.domain.lecture.repository.LectureRepository;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Mock
    private EnrollmentAccessValidator enrollmentAccessValidator;

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
    void 수강권이_있으면_강의에_입장한다() {
        Long courseId = 10L;
        Long lectureId = 50L;
        Long userId = 1L;
        Lecture lecture = lectureWithCourse(lectureId, courseId);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));

        LectureEnterResponse response = lectureService.enterLecture(lectureId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        verify(enrollmentAccessValidator).validateActiveEnrollment(userId, courseId);
        verify(lectureProgressService).ensureStarted(eq(userId), eq(lecture), any());
        verify(lastWatchedLectureService).record(userId, courseId, lectureId);
    }

    @Test
    void 수강권이_없으면_강의_입장에_실패하고_진행행을_생성하지_않는다() {
        Long courseId = 10L;
        Long lectureId = 50L;
        Long userId = 1L;
        Lecture lecture = lectureWithCourse(lectureId, courseId);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        willThrow(new CustomException(ErrorCode.ENROLLMENT_ACCESS_DENIED))
                .given(enrollmentAccessValidator).validateActiveEnrollment(userId, courseId);

        assertThatThrownBy(() -> lectureService.enterLecture(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_ACCESS_DENIED);

        verify(lectureProgressService, never()).ensureStarted(any(), any(), any());
    }

    @Test
    void 존재하지_않는_강의_입장_시_예외가_발생한다() {
        given(lectureRepository.findByIdWithChapterAndCourse(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureService.enterLecture(999L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND);
    }

    private Lecture lectureWithCourse(Long lectureId, Long courseId) {
        Course course = TestEntityFactory.course(courseId);
        ReflectionTestUtils.setField(course, "id", courseId);
        Chapter chapter = ChapterFixture.chapter(1L, "보안 기본", 1, course);
        return LectureFixture.lecture(lectureId, "강의1", "videos/1.m3u8", 600, 1, chapter);
    }
}