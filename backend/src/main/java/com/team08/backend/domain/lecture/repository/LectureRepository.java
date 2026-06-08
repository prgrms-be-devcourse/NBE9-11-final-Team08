package com.team08.backend.domain.lecture.repository;

import com.team08.backend.domain.lecture.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    Optional<Lecture> findFirstByChapterIdOrderByOrderNoAsc(Long chapterId);

    long countByChapterCourseId(Long courseId);
    List<Lecture> findByChapterIdOrderByOrderNoAsc(Long chapterId);
}
