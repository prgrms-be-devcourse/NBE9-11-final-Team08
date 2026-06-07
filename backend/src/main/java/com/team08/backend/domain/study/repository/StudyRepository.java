package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {

    @Query("""
        SELECT DISTINCT s
        FROM Study s
        JOIN FETCH s.owner
        LEFT JOIN StudyMember sm
            ON sm.study = s
            AND sm.user.id = :userId
            AND sm.status = 'ACTIVE'
        WHERE s.deletedAt IS NULL
            AND (
                s.visibility = 'PUBLIC'
                OR sm.id IS NOT NULL
            )
    """)
    List<Study> findVisibleStudiesWithOwner();

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        LEFT JOIN FETCH s.course
        LEFT JOIN StudyMember sm
            ON sm.study = s
            AND sm.user.id = :userId
            AND sm.status = 'ACTIVE'
        WHERE s.deletedAt IS NULL
            AND (
                s.visibility = 'PUBLIC'
                OR sm.id IS NOT NULL
            )
    """)
    Optional<Study> findVisibleStudyByIdWithOwnerAndCourse(Long id);

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        WHERE s.id = :id
        AND s.deletedAt IS NULL
    """)
    Optional<Study> findActiveStudyByIdWithOwner(Long id);

    @Query("""
        SELECT s
        FROM Study s
        WHERE s.id = :id
        AND s.deletedAt IS NULL
    """)
    Optional<Study> findActiveStudyById(Long id);
}
