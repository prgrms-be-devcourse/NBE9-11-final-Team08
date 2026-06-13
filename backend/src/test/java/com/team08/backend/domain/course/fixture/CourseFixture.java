package com.team08.backend.domain.course.fixture;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import org.springframework.test.util.ReflectionTestUtils;

public final class CourseFixture {

    private CourseFixture() {
    }

    public static Course course(Long instructorId, CourseCreateRequest request) {
        return Course.builder()
                .instructorId(instructorId)
                .categoryId(request.categoryId())
                .title(request.title())
                .description(request.description())
                .thumbnail(request.thumbnail())
                .price(request.price())
                .status(CourseStatus.DRAFT)
                .build();
    }

    public static Course course(Long id, Long instructorId, CourseCreateRequest request) {
        Course course = course(instructorId, request);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}