package com.team08.backend.domain.aifeedback.repository;

import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findByStudyActivityId(Long studyActivityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(readOnly = true)
    @Query("""
            select af
            from AiFeedback af
            where af.studyActivityId = :studyActivityId
            """)
    Optional<AiFeedback> findByStudyActivityIdForUpdate(Long studyActivityId);
}
