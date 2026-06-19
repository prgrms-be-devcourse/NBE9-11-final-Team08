package com.team08.backend.domain.learningevent.repository;

import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {

    // ── 중복 이벤트 방지 ──────────────────────────────────────────────
    boolean existsByUniqueEventKey(String uniqueEventKey);

    // ── 강의별 통계 단일 쿼리 ─────────────────────────────────────────
    @Query("SELECT new com.team08.backend.domain.learningevent.dto.CourseStatsProjection(" +
           "SUM(CASE WHEN e.eventType = com.team08.backend.domain.learningevent.entity.LearningEventType.LECTURE_ENTER THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.eventType = com.team08.backend.domain.learningevent.entity.LearningEventType.VIDEO_END THEN e.positionSeconds ELSE 0 END), " +
           "SUM(CASE WHEN e.eventType = com.team08.backend.domain.learningevent.entity.LearningEventType.LECTURE_COMPLETE THEN 1 ELSE 0 END)) " +
           "FROM LearningEvent e WHERE e.courseId = :courseId")
    CourseStatsProjection getStatsByCourseId(@Param("courseId") Long courseId);

    // ── 사용자별 활동 조회 ────────────────────────────────────────────
    Page<LearningEvent> findByUserId(Long userId, Pageable pageable);

    // ── 수강일 수 (이벤트 발생 날짜 기준) ────────────────────────────
    @Query(value = """
            SELECT COUNT(DISTINCT DATE(event_time))
            FROM learning_events
            WHERE user_id = :userId AND course_id = :courseId
            """, nativeQuery = true)
    Integer countStudyDaysByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ── 강의별 시청 시간 (TOP3용) ─────────────────────────────────────
    @Query(value = """
            SELECT lecture_id, SUM(position_seconds) AS total
            FROM learning_events
            WHERE user_id = :userId AND course_id = :courseId AND event_type = 'VIDEO_END'
            GROUP BY lecture_id
            ORDER BY total DESC
            LIMIT 3
            """, nativeQuery = true)
    List<Object[]> findTopLecturesByWatchTime(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ── 날짜별 LECTURE_COMPLETE 누적 (진도 그래프용) ──────────────────
    @Query(value = """
            SELECT DATE(event_time) AS dt, COUNT(*) AS cnt
            FROM learning_events
            WHERE user_id = :userId AND course_id = :courseId AND event_type = 'LECTURE_COMPLETE'
            GROUP BY DATE(event_time)
            ORDER BY dt
            """, nativeQuery = true)
    List<Object[]> findDailyCompletionCounts(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ── 날짜별 이벤트 수 (캘린더 잔디용) ────────────────────────────
    @Query(value = """
            SELECT DATE(event_time) AS dt, COUNT(*) AS cnt
            FROM learning_events
            WHERE user_id = :userId AND course_id = :courseId
            GROUP BY DATE(event_time)
            """, nativeQuery = true)
    List<Object[]> findDailyActivityCounts(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ── 사용자+강좌 기준 총 시청 시간 ────────────────────────────────
    @Query(value = """
            SELECT COALESCE(SUM(position_seconds), 0)
            FROM learning_events
            WHERE user_id = :userId AND course_id = :courseId AND event_type = 'VIDEO_END'
            """, nativeQuery = true)
    Integer sumWatchTimeByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ── 챕터별 이벤트 타입 카운트 ────────────────────────────────────
    long countByChapterIdAndEventType(Long chapterId, LearningEventType eventType);

    @Query("SELECT COALESCE(AVG(e.positionSeconds), 0) FROM LearningEvent e " +
           "WHERE e.chapterId = :chapterId AND e.eventType = 'VIDEO_END'")
    double avgWatchTimeSecondsByChapterId(@Param("chapterId") Long chapterId);

    // ── 판매자 강좌 필터링 ───────────────────────────────────────────
    Page<LearningEvent> findByCourseIdIn(List<Long> courseIds, Pageable pageable);

    @Query("SELECT COALESCE(COUNT(e), 0) FROM LearningEvent e " +
           "WHERE e.courseId IN :courseIds AND e.eventType = :eventType")
    long countByCourseIdInAndEventType(
            @Param("courseIds") List<Long> courseIds,
            @Param("eventType") LearningEventType eventType
    );
}
