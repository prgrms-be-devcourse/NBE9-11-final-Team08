package com.team08.backend.domain.dashboard.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.dashboard.dto.LectureReplayResponse;
import com.team08.backend.domain.dashboard.dto.SellerAnalyticsResponse;
import com.team08.backend.domain.dashboard.dto.SellerCourseDetailResponse;
import com.team08.backend.domain.dashboard.repository.SellerDashboardQueryRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 판매자 센터 "판매 분석" 대시보드 집계 서비스.
 * 호출한 판매자 본인(sellerId) 강좌만 대상으로 매출·수강생·카테고리·인기상품을 계산하고,
 * 단일 강좌 드릴다운(강의별 참여도)과 강의 "자주 본 구간" 히트맵을 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private static final int TOP_COURSES_LIMIT = 5;
    private static final int DEFAULT_REPLAY_BINS = 40;
    private static final int MAX_REPLAY_BINS = 200;
    private static final int REPLAY_EVENT_LIMIT = 50_000; // 단일 강의 페어링 상한(과도한 적재 방어)
    private static final int HOTSPOT_LIMIT = 3;

    private final SellerDashboardQueryRepository queryRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;

    /**
     * @param sellerId 로그인한 판매자(=강좌 instructorId)
     * @param months   조회 개월 수(3/6/12 등). 월별 시계열은 이 개월 수만큼 0으로 채워 연속 반환.
     */
    public SellerAnalyticsResponse getAnalytics(Long sellerId, int months) {
        MonthlySeries series = monthlySeries(queryRepository.monthlyRevenue(sellerId, fromOf(months)), span(months));

        long totalStudents = queryRepository.totalStudents(sellerId);
        long totalCourses = courseRepository.countByInstructorId(sellerId);
        long onSaleCourses = courseRepository.countByInstructorIdAndStatus(sellerId, CourseStatus.ON_SALE);

        List<SellerAnalyticsResponse.CategorySlice> categories = queryRepository.categoryDistribution(sellerId);
        List<SellerAnalyticsResponse.TopCourse> topCourses = queryRepository.topCourses(sellerId, TOP_COURSES_LIMIT);
        List<SellerAnalyticsResponse.CourseBreakdownRow> courseBreakdown = queryRepository.courseBreakdown(sellerId);

        return new SellerAnalyticsResponse(
                series.totalRevenue, series.totalOrders, totalStudents, totalCourses, onSaleCourses,
                series.revenueDelta, series.ordersDelta, series.monthly, categories, topCourses, courseBreakdown);
    }

    /** 단일 강좌 드릴다운: 강좌 한정 KPI·월별 추이·강의별 참여도. */
    public SellerCourseDetailResponse getCourseDetail(Long sellerId, Long courseId, int months) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        course.validateOwner(sellerId); // 본인 강좌가 아니면 UNAUTHORIZED_COURSE_OWNER

        MonthlySeries series = monthlySeries(
                queryRepository.courseMonthlyRevenue(courseId, fromOf(months)), span(months));

        long activeStudents = queryRepository.courseActiveStudents(courseId);
        long completions = queryRepository.courseCompletions(courseId);
        List<SellerCourseDetailResponse.LectureEngagement> lectures = queryRepository.lectureEngagement(courseId);

        return new SellerCourseDetailResponse(
                course.getId(), course.getTitle(), course.getStatus().name(), course.getPrice(),
                series.totalRevenue, series.totalOrders, activeStudents, completions,
                series.revenueDelta, series.ordersDelta, series.monthly, lectures);
    }

    /**
     * 강의 "자주 본 구간" 히트맵.
     * VIDEO_START → VIDEO_END 를 사용자별로 페어링해 시청 구간을 만들고, 강의 길이를 bins 개로 나눠
     * 각 구간을 덮은 시청 횟수를 센다. heat 는 최대 구간 대비 0~1 정규화 값.
     */
    public LectureReplayResponse getLectureReplay(Long sellerId, Long lectureId, int requestedBins) {
        Course course = courseRepository.findByLectureId(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
        course.validateOwner(sellerId);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
        int duration = lecture.getDurationSeconds();
        int bins = clampBins(requestedBins);
        double binSize = (double) duration / bins;

        // 사용자별 VIDEO_START → VIDEO_END 페어링 → 시청 구간 [a,b]
        List<Object[]> rows = queryRepository.replayEvents(lectureId, REPLAY_EVENT_LIMIT);
        Map<Long, Integer> pendingStart = new HashMap<>();
        List<int[]> intervals = new ArrayList<>();
        java.util.Set<Long> viewers = new java.util.HashSet<>();
        for (Object[] r : rows) {
            long userId = ((Number) r[0]).longValue();
            String type = String.valueOf(r[1]);
            Integer pos = (r[2] == null) ? null : ((Number) r[2]).intValue();
            viewers.add(userId);
            if (pos == null) {
                continue;
            }
            if ("VIDEO_START".equals(type)) {
                pendingStart.put(userId, pos);
            } else { // VIDEO_END
                Integer start = pendingStart.remove(userId);
                if (start != null) {
                    int a = Math.max(0, Math.min(start, pos));
                    int b = Math.min(duration, Math.max(start, pos));
                    if (b > a) {
                        intervals.add(new int[]{a, b});
                    }
                }
            }
        }

        // 구간을 bins 버킷에 누적
        long[] counts = new long[bins];
        for (int[] iv : intervals) {
            int lo = (int) Math.floor(iv[0] / binSize);
            int hi = (int) Math.floor((iv[1] - 1e-9) / binSize);
            lo = Math.max(0, Math.min(lo, bins - 1));
            hi = Math.max(0, Math.min(hi, bins - 1));
            for (int i = lo; i <= hi; i++) {
                counts[i]++;
            }
        }
        long max = 0;
        for (long c : counts) {
            max = Math.max(max, c);
        }

        List<LectureReplayResponse.Bin> binList = new ArrayList<>(bins);
        for (int i = 0; i < bins; i++) {
            int startSec = (int) Math.round(i * binSize);
            int endSec = (i == bins - 1) ? duration : (int) Math.round((i + 1) * binSize);
            double heat = (max == 0) ? 0.0 : Math.round(counts[i] * 1000.0 / max) / 1000.0;
            binList.add(new LectureReplayResponse.Bin(i, startSec, endSec, counts[i], heat));
        }

        List<LectureReplayResponse.Hotspot> hotspots = binList.stream()
                .filter(b -> b.count() > 0)
                .sorted(Comparator.comparingLong(LectureReplayResponse.Bin::count).reversed())
                .limit(HOTSPOT_LIMIT)
                .map(b -> new LectureReplayResponse.Hotspot(b.startSeconds(), b.endSeconds(), b.count(), b.heat()))
                .toList();

        return new LectureReplayResponse(
                lectureId, lecture.getTitle(), duration, (int) Math.round(binSize),
                intervals.size(), viewers.size(), binList, hotspots);
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private static int span(int months) {
        return Math.max(1, months);
    }

    private static java.time.LocalDateTime fromOf(int months) {
        YearMonth firstMonth = YearMonth.now().minusMonths(span(months) - 1L);
        LocalDate from = firstMonth.atDay(1);
        return from.atStartOfDay();
    }

    private static int clampBins(int requested) {
        if (requested <= 0) {
            return DEFAULT_REPLAY_BINS;
        }
        return Math.min(requested, MAX_REPLAY_BINS);
    }

    /** byMonth(yyyy-MM → [revenue,orders])를 span 개월 연속 시계열로 정규화하고 합계·증감률을 계산. */
    private MonthlySeries monthlySeries(Map<String, long[]> byMonth, int span) {
        YearMonth firstMonth = YearMonth.now().minusMonths(span - 1L);
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
        double revenueDelta = 0.0;
        double ordersDelta = 0.0;
        if (monthly.size() >= 2) {
            SellerAnalyticsResponse.MonthlyPoint last = monthly.get(monthly.size() - 1);
            SellerAnalyticsResponse.MonthlyPoint prev = monthly.get(monthly.size() - 2);
            revenueDelta = percentDelta(prev.revenue(), last.revenue());
            ordersDelta = percentDelta(prev.orders(), last.orders());
        }
        return new MonthlySeries(monthly, totalRevenue, totalOrders, revenueDelta, ordersDelta);
    }

    /** 직전 대비 증감률(%). 직전이 0이면 증가 시 100%, 동일 0이면 0%로 본다. */
    private static double percentDelta(long previous, long latest) {
        if (previous == 0) {
            return latest > 0 ? 100.0 : 0.0;
        }
        return Math.round((latest - previous) * 1000.0 / previous) / 10.0;
    }

    private record MonthlySeries(
            List<SellerAnalyticsResponse.MonthlyPoint> monthly,
            long totalRevenue,
            long totalOrders,
            double revenueDelta,
            double ordersDelta) {
    }
}
