package com.team08.backend.domain.dashboard.dto;

/**
 * 플랫폼 전체 현황(Overview) KPI.
 */
public record OverviewResponse(
        long totalUsers,
        long sellerCount,
        long regularUserCount,
        long onSaleCourseCount,
        long activeEnrollmentCount,
        long totalLearningEvents,
        long totalCompletions,
        long todaySessions,
        long todayActiveLearners
) {
}
