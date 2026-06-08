package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.StudyApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyApplicationRepository extends JpaRepository<StudyApplication, Long> {

    List<StudyApplication> findByStudyIdOrderByAppliedAtAsc(Long studyId);

    Optional<StudyApplication> findByIdAndStudyId(Long id, Long studyId);

    Optional<StudyApplication> findByStudyIdAndUserId(Long studyId, Long userId);

    boolean existsByStudyIdAndUserId(Long studyId, Long userId);
}
