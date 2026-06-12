package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByCourseId(Long courseId);

    Optional<Study> findByIdAndStatusNot(Long id, StudyStatus status);

    Optional<Study> findByCourseIdAndStatusNot(Long courseId, StudyStatus status);

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        JOIN StudyMember sm ON sm.study = s
        WHERE sm.user.id = :userId
            AND sm.status = 'ACTIVE'
            AND s.status = 'ACTIVE'
    """)
    List<Study> findActiveStudiesByMemberUserId(Long userId);
}
