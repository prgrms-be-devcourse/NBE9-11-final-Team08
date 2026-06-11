package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
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

    @InjectMocks
    private CourseService courseService;

    @Test
    void 강좌를_성공적으로_생성하고_ID를_반환한다() {
        Long instructorId = 1L;
        CourseCreateRequest request = new CourseCreateRequest(
                "테스트 강좌",
                "강좌 설명입니다.",
                10L,
                15000,
                "thumbnail/path.png",
                CourseStatus.DRAFT
        );

        Course savedCourse = Course.builder()
                .instructorId(instructorId)
                .categoryId(request.getCategoryId())
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnail(request.getThumbnail())
                .price(request.getPrice())
                .status(request.getStatus())
                .build();

        java.lang.reflect.Field idField;
        try {
            idField = Course.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedCourse, 100L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(courseRepository.save(any(Course.class))).willReturn(savedCourse);

        Long courseId = courseService.createCourse(instructorId, request);

        assertThat(courseId).isEqualTo(100L);
        verify(courseRepository).save(any(Course.class));
    }
}