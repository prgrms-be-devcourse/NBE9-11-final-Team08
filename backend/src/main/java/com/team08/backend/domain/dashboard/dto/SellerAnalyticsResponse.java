package com.team08.backend.domain.dashboard.dto;

import java.util.List;

/**
 * 판매자 센터 "판매 분석" 화면 한 번 렌더에 필요한 집계를 묶은 응답.
 * 모든 수치는 호출한 판매자(instructorId) 본인의 강좌만으로 한정한다.
 *
 * <ul>
 *   <li>{@code totalRevenue}/{@code totalOrders}: 선택 기간(range) 내 결제완료(PAID) 기준 합계</li>
 *   <li>{@code revenueDelta}/{@code ordersDelta}: 최근 달 대비 직전 달 증감률(%)</li>
 *   <li>{@code totalStudents}: 본인 강좌의 ACTIVE 수강생 수(기간 무관 현재 시점)</li>
 *   <li>{@code monthly}: 월별 매출/판매 건수(빈 달은 0으로 채운 연속 시계열)</li>
 *   <li>{@code categories}: 카테고리별 수강생 비중</li>
 *   <li>{@code topCourses}: 수강생 수 기준 상위 강좌</li>
 * </ul>
 */
public record SellerAnalyticsResponse(
        long totalRevenue,
        long totalOrders,
        long totalStudents,
        long totalCourses,
        long onSaleCourses,
        double revenueDelta,
        double ordersDelta,
        List<MonthlyPoint> monthly,
        List<CategorySlice> categories,
        List<TopCourse> topCourses
) {

    /** 월별 매출/판매 건수 한 점. month 는 "6월" 같은 한글 라벨. */
    public record MonthlyPoint(String month, long revenue, long orders) {
    }

    /** 카테고리별 수강생 비중. */
    public record CategorySlice(String name, long value) {
    }

    /** 인기 상품 행. */
    public record TopCourse(Long courseId, String title, int price, long studentCount, long revenue) {
    }
}
