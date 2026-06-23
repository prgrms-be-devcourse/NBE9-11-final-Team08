package com.team08.backend.domain.course.repository;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c.id FROM Course c WHERE c.instructorId = :instructorId")
    List<Long> findIdsByInstructorId(@Param("instructorId") Long instructorId);

    // MultipleBagFetchException 회피: chapters 와 lectures(두 List bag)를 한 쿼리에서 동시에 fetch 할 수 없어
    // (1) 코스+챕터, (2) 챕터+강의 를 각각 fetch 한다. 같은 영속성 컨텍스트라 (2)가 (1)의 챕터에 강의를 채운다.
    @Query("select distinct c from Course c " +
            "left join fetch c.chapters ch " +
            "where c.id = :courseId " +
            "order by ch.orderNo asc")
    Optional<Course> findWithChaptersAsc(@Param("courseId") Long courseId);

    @Query("select distinct ch from Chapter ch " +
            "left join fetch ch.lectures l " +
            "where ch.course.id = :courseId " +
            "order by ch.orderNo asc, l.orderNo asc")
    List<Chapter> findChaptersWithLecturesAsc(@Param("courseId") Long courseId);

    @Modifying(clearAutomatically = true)
    @Query("update Course c set c.viewCount = c.viewCount + 1 where c.id = :courseId")
    void increaseViewCountAtomic(@Param("courseId") Long courseId);

    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN Chapter ch ON ch.course = c JOIN Lecture l ON l.chapter = ch WHERE l.id = :lectureId")
    Optional<Course> findByLectureId(@Param("lectureId") Long lectureId);

    @Query("SELECT c FROM Course c WHERE c.instructorId = :instructorId AND (:status IS NULL OR c.status = :status)")
    Page<Course> findAllByInstructorIdAndStatus(@Param("instructorId") Long instructorId, @Param("status") CourseStatus status, Pageable pageable);
}