package com.team08.backend.domain.enrollment.repository;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentStatus status);
    boolean existsByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    List<Enrollment> findAllByOrder_IdAndStatus(Long orderId, EnrollmentStatus status);
}
