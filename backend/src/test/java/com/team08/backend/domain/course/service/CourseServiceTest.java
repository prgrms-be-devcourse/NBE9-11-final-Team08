package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.service.CourseStudyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseStudyManager courseStudyManager;

    @InjectMocks
    private CourseService courseService;

    @Test
    void 강좌를_성공적으로_생성하고_ID를_반환한다() {
        // given
        Long instructorId = 1L;
        CourseCreateRequest request = new CourseCreateRequest(
                "테스트 강좌",
                "강좌 설명입니다.",
                10L,
                15000,
                "thumbnail/path.png",
                CourseStatus.DRAFT
        );

        Course savedCourse = Course.forTest(
                100L,
                instructorId,
                request.categoryId(),
                request.title(),
                request.description(),
                request.thumbnail(),
                request.price(),
                request.status()
                );

        given(courseRepository.save(any(Course.class))).willReturn(savedCourse);
        given(courseStudyManager.createForCourse(any())).willReturn(1L);

        // when
        Long courseId = courseService.createCourse(instructorId, request);

        // then
        assertThat(courseId).isEqualTo(100L);
        verify(courseRepository).save(any(Course.class));
        verify(courseStudyManager).createForCourse(any(CourseStudyCreateCommand.class));
    }
}