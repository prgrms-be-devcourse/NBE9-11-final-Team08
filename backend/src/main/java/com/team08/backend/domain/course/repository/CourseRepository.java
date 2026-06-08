package com.team08.backend.domain.course.repository;

import com.team08.backend.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("select distinct c from Course c " +
            "left join fetch c.chapters ch " +
            "left join fetch ch.lectures " +
            "where c.id = :courseId and c.deletedAt is null")
    Optional<Course> findWithCurriculumById(@Param("courseId") Long courseId);
}