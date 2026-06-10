package com.team08.backend.domain.studyactivity.repository;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {
}
