package com.team08.backend.domain.lecture.repository;

import com.team08.backend.domain.lecture.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findByChapterIdOrderByOrderNoAsc(Long chapterId);
}