package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.service.CourseStudyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseStudyManager courseStudyManager;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request) {
        Course course = request.toEntity(instructorId);

        Course savedCourse = courseRepository.save(course);

        courseStudyManager.createForCourse(new CourseStudyCreateCommand(
                instructorId,
                savedCourse.getId(),
                savedCourse.getTitle(),
                savedCourse.getDescription()
        ));

        return savedCourse.getId();
    }
}