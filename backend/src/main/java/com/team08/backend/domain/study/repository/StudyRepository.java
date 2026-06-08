package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        WHERE s.deletedAt IS NULL
          AND (
              s.visibility = 'PUBLIC'
              OR EXISTS (
                  SELECT 1
                  FROM StudyMember sm
                  WHERE sm.study = s
                    AND sm.user.id = :userId
                    AND sm.status = 'ACTIVE'
              )
          )
    """)
    List<Study> findVisibleStudiesWithOwner(@Param("userId") Long userId);

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        LEFT JOIN FETCH s.course
        WHERE s.id = :studyId
          AND s.deletedAt IS NULL
          AND (
              s.visibility = 'PUBLIC'
              OR (
                  :userId IS NOT NULL
                  AND EXISTS (
                      SELECT 1
                      FROM StudyMember sm
                      WHERE sm.study = s
                        AND sm.user.id = :userId
                        AND sm.status = 'ACTIVE'
                  )
              )
          )
    """)
    Optional<Study> findVisibleStudyByIdWithOwnerAndCourse(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        WHERE s.id = :id
        AND s.deletedAt IS NULL
    """)
    Optional<Study> findActiveStudyByIdWithOwner(@Param("id") Long id);

    @Query("""
        SELECT s
        FROM Study s
        WHERE s.id = :id
        AND s.deletedAt IS NULL
    """)
    Optional<Study> findActiveStudyById(@Param("id") Long id);
}
