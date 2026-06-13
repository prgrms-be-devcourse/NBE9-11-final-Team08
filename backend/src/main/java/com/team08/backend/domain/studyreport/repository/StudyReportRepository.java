package com.team08.backend.domain.studyreport.repository;

import com.team08.backend.domain.studyreport.entity.StudyReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyReportRepository extends JpaRepository<StudyReport, Long> {
}
