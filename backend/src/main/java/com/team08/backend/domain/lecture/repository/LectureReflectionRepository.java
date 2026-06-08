package com.team08.backend.domain.lecture.repository;

import com.team08.backend.domain.lecture.entity.LectureReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureReflectionRepository extends JpaRepository<LectureReflection, Long> {

    Optional<LectureReflection> findByUserIdAndLectureId(Long userId, Long lectureId);
}
