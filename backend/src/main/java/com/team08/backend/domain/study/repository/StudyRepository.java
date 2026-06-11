package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByCourseId(Long courseId);

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
