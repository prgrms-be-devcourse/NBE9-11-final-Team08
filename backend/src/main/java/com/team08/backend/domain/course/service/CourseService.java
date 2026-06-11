package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.service.CourseStudyManager;
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
    private final CourseStudyManager courseStudyManager;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request) {
        Course course = request.toEntity(instructorId);
        Course savedCourse = courseRepository.save(course);

        try {
            courseStudyManager.createForCourse(new CourseStudyCreateCommand(
                    instructorId,
                    savedCourse.getId(),
                    savedCourse.getTitle(),
                    savedCourse.getDescription()
            ));
        } catch (Exception e) {
            log.error("강좌 생성 후 스터디 자동 생성 실패. courseId={}, instructorId={}",
                    savedCourse.getId(), instructorId, e);
        }

        return savedCourse.getId();
    }
}