package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
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
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ChapterService chapterService;

    @Test
    void 챕터를_성공적으로_생성하고_ID를_반환한다() {
        Long courseId = 1L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);
        Course course = TestEntityFactory.course(courseId);

        Chapter savedChapter = ChapterFixture.chapter(10L, request.title(), request.orderNo(), course);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.save(any(Chapter.class))).willReturn(savedChapter);

        Long chapterId = chapterService.createChapter(courseId, request);

        assertThat(chapterId).isEqualTo(10L);
        verify(courseRepository).findById(courseId);
        verify(chapterRepository).save(any(Chapter.class));
    }

    @Test
    void 존재하지_않는_강좌_ID로_챕터_생성_요청_시_예외가_발생한다() {
        Long invalidCourseId = 999L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);

        given(courseRepository.findById(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.createChapter(invalidCourseId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(courseRepository).findById(invalidCourseId);
    }
}