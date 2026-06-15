package com.team08.backend.domain.lecture.repository;

import com.team08.backend.domain.lecture.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // 챕터의 강의를 순서대로 조회
    List<Lecture> findByChapterIdOrderByOrderNoAsc(Long chapterId);

    // 챕터의 첫 번째 강의 조회
    Optional<Lecture> findFirstByChapterIdOrderByOrderNoAsc(Long chapterId);

    // 강좌 내 모든 강의 ID 조회
    @Query("SELECT l.id FROM Lecture l JOIN l.chapter c WHERE c.course.id = :courseId")
    List<Long> findIdsByCourseId(@Param("courseId") Long courseId);
}
