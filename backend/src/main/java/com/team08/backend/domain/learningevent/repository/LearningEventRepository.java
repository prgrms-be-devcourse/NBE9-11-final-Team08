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
