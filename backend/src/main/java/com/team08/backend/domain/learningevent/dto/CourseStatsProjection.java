package com.team08.backend.domain.learningevent.dto;

/**
 * getCourseStats 단일 JPQL 쿼리 결과를 담는 내부 Projection.
 * JPQL SUM은 집계 대상이 없으면 null을 반환하므로, 박싱 타입으로 수신 후 0으로 정규화한다.
 */
public record CourseStatsProjection(
        Long enterCount,
        Long watchTimeSeconds,
        Long completionCount
) {
    public CourseStatsProjection {
        enterCount       = enterCount       != null ? enterCount       : 0L;
        watchTimeSeconds = watchTimeSeconds != null ? watchTimeSeconds : 0L;
        completionCount  = completionCount  != null ? completionCount  : 0L;
    }
}