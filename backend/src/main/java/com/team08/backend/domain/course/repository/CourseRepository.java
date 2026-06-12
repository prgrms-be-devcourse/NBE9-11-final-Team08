package com.team08.backend.domain.course.repository;

import com.team08.backend.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("select distinct c from Course c " +
            "left join fetch c.chapters ch " +
            "left join fetch ch.lectures l " +
            "where c.id = :courseId " +
            "order by ch.orderNo asc, l.orderNo asc")
    Optional<Course> findWithChaptersAndLecturesAsc(@Param("courseId") Long courseId);

    @Modifying(clearAutomatically = true)
    @Query("update Course c set c.viewCount = c.viewCount + 1 where c.id = :courseId")
    void increaseViewCountAtomic(@Param("courseId") Long courseId);
}