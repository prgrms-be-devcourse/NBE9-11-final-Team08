package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request) {
        Course course = request.toEntity(instructorId);
        return courseRepository.save(course).getId();
    }

    @Transactional
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findWithChaptersAndLecturesAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        course.increaseViewCount();

        return CourseDetailResponse.from(course);
    }
}