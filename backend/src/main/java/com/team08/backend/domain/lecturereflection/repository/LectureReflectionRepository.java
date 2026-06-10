package com.team08.backend.domain.lecturereflection.repository;

import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureReflectionRepository extends JpaRepository<LectureReflection, Long> {
}
