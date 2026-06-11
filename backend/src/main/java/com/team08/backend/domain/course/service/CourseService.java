package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request) {
        Course course = Course.builder()
                .instructorId(instructorId)
                .categoryId(request.getCategoryId())
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnail(request.getThumbnail())
                .price(request.getPrice())
                .status(request.getStatus())
                .build();

        return courseRepository.save(course).getId();
    }
}