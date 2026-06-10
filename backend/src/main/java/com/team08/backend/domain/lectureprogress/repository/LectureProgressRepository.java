package com.team08.backend.domain.lectureprogress.repository;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {
}
