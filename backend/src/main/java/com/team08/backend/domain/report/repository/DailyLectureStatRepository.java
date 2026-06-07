package com.team08.backend.domain.report.repository;

import com.team08.backend.domain.report.entity.DailyLectureStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLectureStatRepository extends JpaRepository<DailyLectureStat, Long> {

    Optional<DailyLectureStat> findByUserIdAndCourseIdAndStatDate(Long userId, Long courseId, LocalDate statDate);

    List<DailyLectureStat> findByUserIdAndCourseIdAndStatDateBetweenOrderByStatDateAsc(
            Long userId,
            Long courseId,
            LocalDate startDate,
            LocalDate endDate
    );
}
