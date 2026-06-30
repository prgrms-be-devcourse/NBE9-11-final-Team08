package com.team08.backend.domain.coursestatushistory.repository;

import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.course.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseStatusHistoryRepository extends JpaRepository<CourseStatusHistory, Long> {
    Optional<CourseStatusHistory> findTopByCourseIdAndToStatusOrderByCreatedAtDesc(Long courseId, CourseStatus toStatus);

    Optional<CourseStatusHistory> findTopByCourseIdAndToStatusAndReasonIsNotNullOrderByCreatedAtDesc(Long courseId, CourseStatus toStatus);
}
