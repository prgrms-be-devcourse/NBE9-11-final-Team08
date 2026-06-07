package com.team08.backend.domain.enrollment.service;

import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.dto.CourseAccessResponse;
import com.team08.backend.domain.enrollment.dto.EnrollmentResponse;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(Long userId) {
        return enrollmentRepository.findAllByUserIdAndStatusOrderByEnrolledAtDesc(userId, EnrollmentStatus.ACTIVE).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseAccessResponse canAccess(Long userId, Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다.");
        }

        boolean accessible = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId,
                courseId,
                EnrollmentStatus.ACTIVE
        );
        return new CourseAccessResponse(courseId, accessible);
    }
}
