package com.team08.backend.domain.course.fixture;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import org.springframework.test.util.ReflectionTestUtils;

public final class CourseFixture {

    private CourseFixture() {
    }

    public static Course course(Long instructorId, CourseCreateRequest request) {
        return Course.createDraft(
                instructorId,
                request.categoryId(),
                request.title(),
                request.description(),
                request.thumbnail(),
                request.price()
        );
    }

    public static Course course(Long id, Long instructorId, CourseCreateRequest request) {
        Course course = course(instructorId, request);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}