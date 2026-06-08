package com.team08.backend.domain.chapter.repository;

import com.team08.backend.domain.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourseIdOrderByOrderNoAsc(Long courseId);
}