package com.team08.backend.domain.dashboard.service;

import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.dashboard.dto.SellerAnalyticsResponse;
import com.team08.backend.domain.dashboard.repository.SellerDashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 판매자 센터 "판매 분석" 대시보드 집계 서비스.
 * 호출한 판매자 본인(sellerId) 강좌만 대상으로 매출·수강생·카테고리·인기상품을 한 번에 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private static final int TOP_COURSES_LIMIT = 5;

    private final SellerDashboardQueryRepository queryRepository;
    private final CourseRepository courseRepository;

    /**
     * @param sellerId 로그인한 판매자(=강좌 instructorId)
     * @param months   조회 개월 수(3/6/12 등). 월별 시계열은 이 개월 수만큼 0으로 채워 연속 반환.
     */
    public SellerAnalyticsResponse getAnalytics(Long sellerId, int months) {
        int span = Math.max(1, months);

        YearMonth current = YearMonth.now();
        YearMonth firstMonth = current.minusMonths(span - 1L);
        LocalDate from = firstMonth.atDay(1);

        Map<String, long[]> byMonth = queryRepository.monthlyRevenue(sellerId, from.atStartOfDay());

        // span 개월을 0으로 채운 연속 시계열로 정규화
        List<SellerAnalyticsResponse.MonthlyPoint> monthly = new ArrayList<>(span);
        long totalRevenue = 0;
        long totalOrders = 0;
        for (int i = 0; i < span; i++) {
            YearMonth ym = firstMonth.plusMonths(i);
            long[] v = byMonth.getOrDefault(ym.toString(), new long[]{0L, 0L});
            totalRevenue += v[0];
            totalOrders += v[1];
            monthly.add(new SellerAnalyticsResponse.MonthlyPoint(ym.getMonthValue() + "월", v[0], v[1]));
        }

        // 증감률: 최근 달 vs 직전 달 (span >= 2일 때만 의미 있음)
        double revenueDelta = 0.0;
        double ordersDelta = 0.0;
        if (monthly.size() >= 2) {
            SellerAnalyticsResponse.MonthlyPoint last = monthly.get(monthly.size() - 1);
            SellerAnalyticsResponse.MonthlyPoint prev = monthly.get(monthly.size() - 2);
            revenueDelta = percentDelta(prev.revenue(), last.revenue());
            ordersDelta = percentDelta(prev.orders(), last.orders());
        }

        long totalStudents = queryRepository.totalStudents(sellerId);
        long totalCourses = courseRepository.countByInstructorId(sellerId);
        long onSaleCourses = courseRepository.countByInstructorIdAndStatus(sellerId, CourseStatus.ON_SALE);

        List<SellerAnalyticsResponse.CategorySlice> categories = queryRepository.categoryDistribution(sellerId);
        List<SellerAnalyticsResponse.TopCourse> topCourses = queryRepository.topCourses(sellerId, TOP_COURSES_LIMIT);

        return new SellerAnalyticsResponse(
                totalRevenue, totalOrders, totalStudents, totalCourses, onSaleCourses,
                revenueDelta, ordersDelta, monthly, categories, topCourses);
    }

    /** 직전 대비 증감률(%). 직전이 0이면 증가 시 100%, 동일 0이면 0%로 본다. */
    private static double percentDelta(long previous, long latest) {
        if (previous == 0) {
            return latest > 0 ? 100.0 : 0.0;
        }
        return Math.round((latest - previous) * 1000.0 / previous) / 10.0;
    }
}
