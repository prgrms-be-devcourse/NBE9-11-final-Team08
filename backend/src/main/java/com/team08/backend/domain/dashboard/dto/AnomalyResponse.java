package com.team08.backend.domain.dashboard.dto;

import java.util.List;

/**
 * 이상 데이터 탐지 결과(온디맨드).
 * highIncompletionCourses: 미완강률이 임계값을 초과한 강좌.
 * duplicateBursts: 짧은 시간(분 단위 버킷)에 동일 이벤트가 임계값 초과로 몰린 케이스.
 */
public record AnomalyResponse(
        double incompletionThreshold,
        int burstThreshold,
        int windowMinutes,
        List<CourseStatRow> highIncompletionCourses,
        List<DuplicateBurst> duplicateBursts
) {
    public record DuplicateBurst(
            Long userId,
            Long lectureId,
            String eventType,
            String bucketMinute,
            long count
    ) {
    }
}
