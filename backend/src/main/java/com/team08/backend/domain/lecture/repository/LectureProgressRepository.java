package com.team08.backend.domain.lecture.repository;

import com.team08.backend.domain.lecture.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {

    Optional<LectureProgress> findByUserIdAndLectureId(Long userId, Long lectureId);

    Optional<LectureProgress> findFirstByUserIdAndLectureChapterIdOrderByUpdatedAtDesc(Long userId, Long chapterId);

    Optional<LectureProgress> findFirstByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("""
            select coalesce(sum(p.watchedSeconds), 0)
            from LectureProgress p
            where p.user.id = :userId
              and p.lecture.chapter.course.id = :courseId
            """)
    Integer sumWatchedSeconds(@Param("userId") Long userId, @Param("courseId") Long courseId);

    long countByUserIdAndLectureChapterCourseIdAndCompletedTrue(Long userId, Long courseId);

    List<LectureProgress> findByUserIdAndLectureChapterCourseId(Long userId, Long courseId);
}
