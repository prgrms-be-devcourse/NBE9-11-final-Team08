package com.team08.backend.domain.chapter.repository;

import com.team08.backend.domain.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourseIdOrderByOrderNoAsc(Long courseId);
}
