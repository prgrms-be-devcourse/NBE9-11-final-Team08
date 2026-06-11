package com.team08.backend.domain.lecturereflection.repository;

import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureReflectionRepository extends JpaRepository<LectureReflection, Long> {
    boolean existsByUserIdAndLectureId(Long userId, Long lectureId);
    Optional<LectureReflection> findByUserIdAndLectureId(Long userId, Long lectureId);
}
