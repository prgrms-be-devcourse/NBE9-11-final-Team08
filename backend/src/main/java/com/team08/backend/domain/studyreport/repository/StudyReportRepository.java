package com.team08.backend.domain.studyreport.repository;

import com.team08.backend.domain.studyreport.entity.StudyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyReportRepository extends JpaRepository<StudyReport, Long> {
    Optional<StudyReport> findByUserIdAndStudyId(Long userId, Long studyId);

    List<StudyReport> findByUserIdAndStudyIdIn(Long userId, List<Long> studyIds);
}
