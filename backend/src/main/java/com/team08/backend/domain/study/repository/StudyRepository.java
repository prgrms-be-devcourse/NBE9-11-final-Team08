package com.team08.backend.domain.study.repository;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByCourseId(Long courseId);

    boolean existsByIdAndStatusNotIn(Long studyId, Collection<StudyStatus> statuses);

    Optional<Study> findByIdAndStatusNot(Long id, StudyStatus status);

    Optional<Study> findByCourseIdAndStatusNot(Long courseId, StudyStatus status);

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.course
        WHERE s.id = :studyId
    """)
    Optional<Study> findByIdWithCourse(@Param("studyId") Long studyId);

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        JOIN StudyMember sm ON sm.study = s
        JOIN Course c ON c = s.course
        JOIN Enrollment e ON e.courseId = c.id AND e.userId = :userId
        WHERE e.status = 'ACTIVE'
            AND sm.user.id = :userId
            AND sm.status = 'ACTIVE'
            AND s.status IN ('ACTIVE', 'READONLY')
    """)
    List<Study> findVisibleStudiesByUserId(Long userId);

    @Query("""
        SELECT s
        FROM Study s
        JOIN FETCH s.owner
        JOIN Course c ON c = s.course
        WHERE c.id = :courseId
    """)
    Optional<Study> findByCourseIdWithCourse(Long courseId);
}
