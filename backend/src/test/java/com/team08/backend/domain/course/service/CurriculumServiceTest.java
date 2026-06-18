package com.team08.backend.domain.course.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.dto.ChapterReorderRequest;
import com.team08.backend.domain.course.dto.LectureReorderRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CurriculumServiceTest {

    @InjectMocks
    private CurriculumService curriculumService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private CurriculumValidator curriculumValidator;

    @Test
    void 강좌_내_챕터_순서가_정상적으로_일괄_변경된다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);
        Chapter chapter = mock(Chapter.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findByCourseIdOrderByOrderNo(courseId)).willReturn(List.of(chapter));
        given(chapter.getId()).willReturn(10L);

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1)
        ));

        curriculumService.reorderChapters(courseId, instructorId, request);

        verify(chapterRepository).updateOrderNo(10L, 1, courseId);
    }

    @Test
    void 요청한_챕터ID_개수와_DB에서_조회된_전체_개수가_일치하지_않으면_예외를_던진다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findByCourseIdOrderByOrderNo(courseId)).willReturn(List.of());
        doThrow(CustomException.class).when(curriculumValidator).validateSize(anyInt(), anyInt());

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1),
                new ChapterReorderRequest.ChapterOrderElement(20L, 2)
        ));

        assertThatThrownBy(() -> curriculumService.reorderChapters(courseId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 다른_강좌에_속한_챕터ID가_요청에_포함되는_정합성_오류_시_예외를_던진다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);
        Chapter chapter = mock(Chapter.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findByCourseIdOrderByOrderNo(courseId)).willReturn(List.of(chapter));
        given(chapter.getId()).willReturn(999L);
        doThrow(CustomException.class).when(curriculumValidator).validateIds(any(), any());

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1)
        ));

        assertThatThrownBy(() -> curriculumService.reorderChapters(courseId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 챕터_순서_번호가_1부터_시작하는_연속된_정수가_아니라면_예외를_던진다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);
        Chapter chapter = mock(Chapter.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findByCourseIdOrderByOrderNo(courseId)).willReturn(List.of(chapter));
        given(chapter.getId()).willReturn(10L);
        doThrow(CustomException.class).when(curriculumValidator).validateOrderSequence(any());

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 3)
        ));

        assertThatThrownBy(() -> curriculumService.reorderChapters(courseId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 챕터_순서_번호가_중복되면_예외를_던진다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);
        Chapter chapter1 = mock(Chapter.class);
        Chapter chapter2 = mock(Chapter.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findByCourseIdOrderByOrderNo(courseId)).willReturn(List.of(chapter1, chapter2));
        given(chapter1.getId()).willReturn(10L);
        given(chapter2.getId()).willReturn(20L);
        doThrow(CustomException.class).when(curriculumValidator).validateOrderSequence(any());

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1),
                new ChapterReorderRequest.ChapterOrderElement(20L, 1) // 중복
        ));

        assertThatThrownBy(() -> curriculumService.reorderChapters(courseId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 챕터_내_강의_순서가_정상적으로_일괄_변경된다() {
        Long chapterId = 10L;
        Long instructorId = 100L;
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);
        Lecture lecture = mock(Lecture.class);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId)).willReturn(List.of(lecture));
        given(lecture.getId()).willReturn(100L);

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1)
        ));

        curriculumService.reorderLectures(chapterId, instructorId, request);

        verify(lectureRepository).updateOrderNo(100L, 1, chapterId);
    }

    @Test
    void 요청한_강의ID_개수와_DB에서_조회된_전체_개수가_일치하지_않으면_예외를_던진다() {
        Long chapterId = 10L;
        Long instructorId = 100L;
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId)).willReturn(List.of());
        doThrow(CustomException.class).when(curriculumValidator).validateSize(anyInt(), anyInt());

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1),
                new LectureReorderRequest.LectureOrderElement(200L, 2)
        ));

        assertThatThrownBy(() -> curriculumService.reorderLectures(chapterId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 다른_챕터에_속한_강의ID가_요청에_포함되는_정합성_오류_시_예외를_던진다() {
        Long chapterId = 10L;
        Long instructorId = 100L;
        Chapter chapter = mock(Chapter.class);
        Lecture lecture = mock(Lecture.class);
        Course course = mock(Course.class);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId)).willReturn(List.of(lecture));
        given(lecture.getId()).willReturn(999L);
        doThrow(CustomException.class).when(curriculumValidator).validateIds(any(), any());

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1)
        ));

        assertThatThrownBy(() -> curriculumService.reorderLectures(chapterId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 강의_순서_번호가_1부터_시작하는_연속된_정수가_아니라면_예외를_던진다() {
        Long chapterId = 10L;
        Long instructorId = 100L;
        Chapter chapter = mock(Chapter.class);
        Lecture lecture = mock(Lecture.class);
        Course course = mock(Course.class);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId)).willReturn(List.of(lecture));
        given(lecture.getId()).willReturn(100L);
        doThrow(CustomException.class).when(curriculumValidator).validateOrderSequence(any());

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 5)
        ));

        assertThatThrownBy(() -> curriculumService.reorderLectures(chapterId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 강의_순서_번호가_중복되면_예외를_던진다() {
        Long chapterId = 10L;
        Long instructorId = 100L;
        Chapter chapter = mock(Chapter.class);
        Lecture lecture1 = mock(Lecture.class);
        Lecture lecture2 = mock(Lecture.class);
        Course course = mock(Course.class);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId)).willReturn(List.of(lecture1, lecture2));
        given(lecture1.getId()).willReturn(100L);
        given(lecture2.getId()).willReturn(200L);
        doThrow(CustomException.class).when(curriculumValidator).validateOrderSequence(any());

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1),
                new LectureReorderRequest.LectureOrderElement(200L, 1) // 중복
        ));

        assertThatThrownBy(() -> curriculumService.reorderLectures(chapterId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }
}