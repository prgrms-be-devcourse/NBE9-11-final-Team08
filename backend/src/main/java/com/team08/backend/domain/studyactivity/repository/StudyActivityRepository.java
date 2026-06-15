package com.team08.backend.domain.studyactivity.repository;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {
    Page<StudyActivity> findAllByStudyIdAndDeletedAtIsNull(
            Long studyId,
            Pageable pageable
    );

    Optional<StudyActivity> findByIdAndStudyIdAndDeletedAtIsNull(
            Long id,
            Long studyId
    );
}
