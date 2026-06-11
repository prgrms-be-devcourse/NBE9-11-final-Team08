package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseViewCountManager courseViewCountManager;

    @Transactional
    public Long createCourse(Long instructorId, CourseCreateRequest request) {
        Course course = request.toEntity(instructorId);
        return courseRepository.save(course).getId();
    }

    @Transactional
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findWithChaptersAndLecturesAsc(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        // TODO: 대규모 트래픽 발생 시 RDB Write 부하가 우려되므로 차후 Redis를 활용한 쓰기 지연(Write-Behind) 방식으로 고도화 필요
        try {
            courseViewCountManager.increaseViewCountRequiresNew(courseId);
        } catch (Exception e) {
            log.error("Failed to increase course view count for courseId: {}", courseId, e);
        }

        return CourseDetailResponse.from(course);
    }

    public Page<CourseCardResponse> getCourses(Pageable pageable) {
        return courseRepository.findAllByStatus(CourseStatus.ON_SALE, pageable)
                .map(CourseCardResponse::from);
    }

    @Component
    @RequiredArgsConstructor
    public static class CourseViewCountManager {

        private final CourseRepository courseRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void increaseViewCountRequiresNew(Long courseId) {
            courseRepository.increaseViewCountAtomic(courseId);
        }
    }
}