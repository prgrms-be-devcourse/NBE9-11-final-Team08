package com.team08.backend.domain.dashboard.dto;

import java.util.List;

/**
 * 판매자 "판매 분석" 단일 강좌 드릴다운 응답.
 * 강좌 단위로 한정한 매출/판매/수강생 KPI·월별 추이·강의별 참여도를 담는다.
 *
 * <ul>
 *   <li>{@code totalRevenue}/{@code totalOrders}: 선택 기간(range) 내 결제완료(PAID) 합계</li>
 *   <li>{@code activeStudents}: 현재 ACTIVE 수강생</li>
 *   <li>{@code completions}: LECTURE_COMPLETE 이벤트 수(강좌 전체 누적)</li>
 *   <li>{@code monthly}: 강좌 기준 월별 매출/판매 건수(빈 달 0으로 채운 연속 시계열)</li>
 *   <li>{@code lectures}: 강의별 참여도(입장/완료/시청자/평균 시청 위치)</li>
 * </ul>
 */
public record SellerCourseDetailResponse(
        Long courseId,
        String title,
        String status,
        int price,
        long totalRevenue,
        long totalOrders,
        long activeStudents,
        long completions,
        double revenueDelta,
        double ordersDelta,
        List<SellerAnalyticsResponse.MonthlyPoint> monthly,
        List<LectureEngagement> lectures
) {

    /** 강의별 참여도 한 행. avgWatchSeconds 는 VIDEO_PAUSE 의 평균 멈춤 위치(초). */
    public record LectureEngagement(
            Long lectureId,
            String chapterTitle,
            String title,
            int durationSeconds,
            long enterCount,
            long completeCount,
            long viewerCount,
            long avgWatchSeconds
    ) {
    }
}
