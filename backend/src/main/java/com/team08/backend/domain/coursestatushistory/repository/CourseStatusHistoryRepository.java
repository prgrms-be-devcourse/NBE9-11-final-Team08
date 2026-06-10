package com.team08.backend.domain.coursestatushistory.repository;

import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStatusHistoryRepository extends JpaRepository<CourseStatusHistory, Long> {
}
