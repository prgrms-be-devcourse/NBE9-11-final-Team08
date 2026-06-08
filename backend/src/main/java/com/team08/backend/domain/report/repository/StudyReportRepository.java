package com.team08.backend.domain.report.repository;

import com.team08.backend.domain.report.entity.StudyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyReportRepository extends JpaRepository<StudyReport, Long> {

    Optional<StudyReport> findTopByUserIdAndCourseIdOrderByGeneratedAtDesc(Long userId, Long courseId);
}
