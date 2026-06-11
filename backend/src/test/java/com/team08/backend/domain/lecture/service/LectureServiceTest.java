package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LectureServiceTest {

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @InjectMocks
    private LectureService lectureService;

    @Test
    void 강의를_성공적으로_생성하고_ID를_반환한다() {
        Long courseId = 10L;
        Long chapterId = 1L;
        LectureCreateRequest request = new LectureCreateRequest(
                "스프링 시큐리티 구조와 흐름",
                "videos/security.m3u8",
                "시큐리티 필터 체인 분석",
                1200,
                1,
                false
        );

        Course course = TestEntityFactory.course(courseId);
        Chapter chapter = ChapterFixture.chapter(chapterId, "보안 기본", 1, course);
        Lecture savedLecture = LectureFixture.lecture(50L, request.title(), request.m3u8Path(), request.durationSeconds(), request.orderNo(), chapter);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        given(lectureRepository.save(any(Lecture.class))).willReturn(savedLecture);

        Long lectureId = lectureService.createLecture(courseId, chapterId, request);

        assertThat(lectureId).isEqualTo(50L);
        verify(chapterRepository).findById(chapterId);
        verify(lectureRepository).save(any(Lecture.class));
    }

    @Test
    void 존재하지_않는_챕터_ID로_강의_생성_요청_시_예외가_발생한다() {
        Long courseId = 10L;
        Long invalidChapterId = 999L;
        LectureCreateRequest request = new LectureCreateRequest(
                "스프링 시큐리티 구조와 흐름",
                "videos/security.m3u8",
                "시큐리티 필터 체인 분석",
                1200,
                1,
                false
        );

        given(chapterRepository.findById(invalidChapterId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lectureService.createLecture(courseId, invalidChapterId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.CHAPTER_NOT_FOUND.getMessage());

        verify(chapterRepository).findById(invalidChapterId);
    }

    @Test
    void 요청된_코스_ID와_챕터의_코스_ID가_일치하지_않으면_예외가_발생한다() {
        Long invalidCourseId = 999L;
        Long chapterId = 1L;
        LectureCreateRequest request = new LectureCreateRequest(
                "스프링 시큐리티 구조와 흐름",
                "videos/security.m3u8",
                "시큐리티 필터 체인 분석",
                1200,
                1,
                false
        );

        Course realCourse = TestEntityFactory.course(10L);
        Chapter chapter = ChapterFixture.chapter(chapterId, "보안 기본", 1, realCourse);

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));

        assertThatThrownBy(() -> lectureService.createLecture(invalidCourseId, chapterId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(chapterRepository).findById(chapterId);
    }
}