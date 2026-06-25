package com.team08.backend.domain.studyreport.repository;

import com.team08.backend.domain.studyreport.entity.StudyDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudyDailyStatRepository extends JpaRepository<StudyDailyStat, Long> {

    /**
     * (user, course, date) 행을 원자적으로 증분한다. 행이 없으면 생성, 있으면 카운트를 더한다.
     * 학습 이벤트마다 1회 호출 — event_count += 1, 완료 이벤트면 completed_count += 1.
     * (MySQL ON DUPLICATE KEY UPDATE / 유니크키 uk_daily_user_course_date 기반)
     */
    @Modifying
    @Query(value = """
            INSERT INTO learning_daily_stats (user_id, course_id, activity_date, event_count, completed_count)
            VALUES (:userId, :courseId, :date, 1, :completedDelta)
            ON DUPLICATE KEY UPDATE
                event_count = event_count + 1,
                completed_count = completed_count + :completedDelta
            """, nativeQuery = true)
    void upsertIncrement(@Param("userId") Long userId,
                         @Param("courseId") Long courseId,
                         @Param("date") LocalDate date,
                         @Param("completedDelta") int completedDelta);

    List<StudyDailyStat> findByUserIdAndCourseIdOrderByActivityDateAsc(Long userId, Long courseId);
}
