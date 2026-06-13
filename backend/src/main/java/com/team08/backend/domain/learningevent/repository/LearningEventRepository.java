package com.team08.backend.domain.learningevent.repository;

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

    // ── 사용자별 활동 조회 ────────────────────────────────────────────
    Page<LearningEvent> findByUserId(Long userId, Pageable pageable);

    // ── 강의별 이벤트 타입 카운트 ─────────────────────────────────────
    long countByCourseIdAndEventType(Long courseId, LearningEventType eventType);

    @Query("SELECT COALESCE(SUM(e.positionSeconds), 0) FROM LearningEvent e " +
           "WHERE e.courseId = :courseId AND e.eventType = 'VIDEO_END'")
    long sumWatchTimeSecondsByCourseId(@Param("courseId") Long courseId);

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
