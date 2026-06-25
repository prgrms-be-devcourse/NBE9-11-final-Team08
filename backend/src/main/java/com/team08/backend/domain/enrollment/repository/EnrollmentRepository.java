package com.team08.backend.domain.enrollment.repository;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentStatus status);
    boolean existsByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    @Query("""
            select e.courseId
            from Enrollment e
            where e.userId = :userId
              and e.status = :status
              and e.courseId in :courseIds
            """)
    List<Long> findCourseIdsByUserIdAndStatusAndCourseIdIn(
            @Param("userId") Long userId,
            @Param("status") EnrollmentStatus status,
            @Param("courseIds") List<Long> courseIds
    );
    @Query("""
            select e.courseId
            from Enrollment e
            where e.userId = :userId
              and e.courseId in :courseIds
            """)
    List<Long> findCourseIdsByUserIdAndCourseIdIn(
            @Param("userId") Long userId,
            @Param("courseIds") List<Long> courseIds
    );
    List<Enrollment> findAllByOrder_IdAndStatus(Long orderId, EnrollmentStatus status);

    long countByStatus(EnrollmentStatus status);
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    List<Enrollment> findAllByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}
