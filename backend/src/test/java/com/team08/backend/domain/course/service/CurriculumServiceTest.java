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
import static org.mockito.Mockito.mock;

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

    @Test
    void 강좌_내_챕터_순서가_정상적으로_일괄_변경된다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);
        Chapter chapter = mock(Chapter.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findAllById(List.of(10L))).willReturn(List.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(course.getId()).willReturn(courseId);

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1)
        ));

        curriculumService.reorderChapters(courseId, instructorId, request);
    }

    @Test
    void 다른_강좌에_속한_챕터ID가_요청에_포함되면_예외를_던진다() {
        Long courseId = 1L;
        Long instructorId = 100L;
        Course course = mock(Course.class);
        Course anotherCourse = mock(Course.class);
        Chapter chapter = mock(Chapter.class);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.findAllById(List.of(10L))).willReturn(List.of(chapter));
        given(chapter.getCourse()).willReturn(anotherCourse);
        given(anotherCourse.getId()).willReturn(999L);

        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1)
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
        given(lectureRepository.findAllById(List.of(100L))).willReturn(List.of(lecture));
        given(lecture.getChapter()).willReturn(chapter);
        given(chapter.getId()).willReturn(chapterId);

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1)
        ));

        curriculumService.reorderLectures(chapterId, instructorId, request);
    }

    @Test
    void 다른_챕터에_속한_강의ID가_요청에_포함되면_예외를_던진다() {
        Long chapterId = 10L;
        Long instructorId = 100L;
        Chapter chapter = mock(Chapter.class);
        Chapter anotherChapter = mock(Chapter.class);
        Lecture lecture = mock(Lecture.class);
        Course course = mock(Course.class);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(chapter.getCourse()).willReturn(course);
        given(lectureRepository.findAllById(List.of(100L))).willReturn(List.of(lecture));
        given(lecture.getChapter()).willReturn(anotherChapter);
        given(anotherChapter.getId()).willReturn(999L);

        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1)
        ));

        assertThatThrownBy(() -> curriculumService.reorderLectures(chapterId, instructorId, request))
                .isInstanceOf(CustomException.class);
    }
}