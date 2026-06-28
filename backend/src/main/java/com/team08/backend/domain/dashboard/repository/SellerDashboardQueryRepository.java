package com.team08.backend.domain.dashboard.repository;

import com.team08.backend.domain.dashboard.dto.SellerAnalyticsResponse;
import com.team08.backend.domain.dashboard.dto.SellerCourseDetailResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 판매자 대시보드 전용 집계 쿼리 모음.
 * 주문/결제·수강·카테고리 테이블을 가로지르는 읽기 전용 네이티브 집계라 EntityManager 로 직접 작성한다.
 * ({@link DashboardQueryRepository} 의 nativeQuery 스타일을 따른다.)
 * 모든 쿼리는 instructorId 로 강좌를 한정해 판매자 본인 데이터만 노출한다.
 */
@Repository
public class SellerDashboardQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 월별(yyyy-MM) 매출·판매 건수. 결제완료(PAID) 주문의 order_items 중 본인 강좌만 집계.
     * 판매 건수는 order_items(강좌 단위 판매) 개수로 센다.
     * 반환: yyyy-MM → [revenue, orders] (해당 월에 판매가 있는 것만, 빈 달 채움은 서비스가 담당)
     */
    public Map<String, long[]> monthlyRevenue(Long sellerId, LocalDateTime from) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT DATE_FORMAT(o.paid_at, '%Y-%m') AS ym,
                       COALESCE(SUM(oi.final_price), 0) AS revenue,
                       COUNT(*) AS orders
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                JOIN courses c ON oi.course_id = c.id
                WHERE o.status = 'PAID'
                  AND o.paid_at >= :from
                  AND c.instructor_id = :sellerId
                  AND c.deleted_at IS NULL
                GROUP BY ym
                ORDER BY ym
                """)
                .setParameter("from", from)
                .setParameter("sellerId", sellerId)
                .getResultList();

        Map<String, long[]> result = new LinkedHashMap<>();
        for (Object[] r : rows) {
            result.put(String.valueOf(r[0]), new long[]{toLong(r[1]), toLong(r[2])});
        }
        return result;
    }

    /** 본인 강좌의 ACTIVE 수강생 총수(현재 시점). */
    public long totalStudents(Long sellerId) {
        return toLong(em.createNativeQuery("""
                SELECT COUNT(*)
                FROM enrollments e
                JOIN courses c ON e.course_id = c.id
                WHERE c.instructor_id = :sellerId
                  AND c.deleted_at IS NULL
                  AND e.status = 'ACTIVE'
                """).setParameter("sellerId", sellerId).getSingleResult());
    }

    /** 카테고리별 수강생 수(ACTIVE). 수강생 0인 카테고리도 포함(LEFT JOIN). */
    public List<SellerAnalyticsResponse.CategorySlice> categoryDistribution(Long sellerId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT cat.name AS name, COUNT(e.id) AS students
                FROM courses c
                JOIN categories cat ON c.category_id = cat.id
                LEFT JOIN enrollments e ON e.course_id = c.id AND e.status = 'ACTIVE'
                WHERE c.instructor_id = :sellerId
                  AND c.deleted_at IS NULL
                GROUP BY cat.name
                ORDER BY students DESC, name
                """).setParameter("sellerId", sellerId).getResultList();

        List<SellerAnalyticsResponse.CategorySlice> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new SellerAnalyticsResponse.CategorySlice(String.valueOf(r[0]), toLong(r[1])));
        }
        return result;
    }

    /** 수강생 수 기준 상위 강좌. revenue 는 누적 결제완료 매출. */
    public List<SellerAnalyticsResponse.TopCourse> topCourses(Long sellerId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT c.id, c.title, c.price,
                  (SELECT COUNT(*) FROM enrollments e
                     WHERE e.course_id = c.id AND e.status = 'ACTIVE') AS students,
                  (SELECT COALESCE(SUM(oi.final_price), 0) FROM order_items oi
                     JOIN orders o ON oi.order_id = o.id
                     WHERE oi.course_id = c.id AND o.status = 'PAID') AS revenue
                FROM courses c
                WHERE c.instructor_id = :sellerId
                  AND c.deleted_at IS NULL
                ORDER BY students DESC, revenue DESC, c.id DESC
                LIMIT :limit
                """)
                .setParameter("sellerId", sellerId)
                .setParameter("limit", limit)
                .getResultList();

        List<SellerAnalyticsResponse.TopCourse> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new SellerAnalyticsResponse.TopCourse(
                    toLong(r[0]), String.valueOf(r[1]), ((Number) r[2]).intValue(),
                    toLong(r[3]), toLong(r[4])));
        }
        return result;
    }

    // ── 단일 강좌 드릴다운 ──────────────────────────────────────────────

    /** 단일 강좌의 월별(yyyy-MM) 매출·판매 건수. {@link #monthlyRevenue}의 강좌 한정 버전. */
    public Map<String, long[]> courseMonthlyRevenue(Long courseId, LocalDateTime from) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT DATE_FORMAT(o.paid_at, '%Y-%m') AS ym,
                       COALESCE(SUM(oi.final_price), 0) AS revenue,
                       COUNT(*) AS orders
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                WHERE o.status = 'PAID'
                  AND o.paid_at >= :from
                  AND oi.course_id = :courseId
                GROUP BY ym
                ORDER BY ym
                """)
                .setParameter("from", from)
                .setParameter("courseId", courseId)
                .getResultList();

        Map<String, long[]> result = new LinkedHashMap<>();
        for (Object[] r : rows) {
            result.put(String.valueOf(r[0]), new long[]{toLong(r[1]), toLong(r[2])});
        }
        return result;
    }

    /** 단일 강좌의 ACTIVE 수강생 수. */
    public long courseActiveStudents(Long courseId) {
        return toLong(em.createNativeQuery("""
                SELECT COUNT(*) FROM enrollments e
                WHERE e.course_id = :courseId AND e.status = 'ACTIVE'
                """).setParameter("courseId", courseId).getSingleResult());
    }

    /** 단일 강좌의 LECTURE_COMPLETE 이벤트 누적 수. */
    public long courseCompletions(Long courseId) {
        return toLong(em.createNativeQuery("""
                SELECT COUNT(*) FROM learning_events le
                WHERE le.course_id = :courseId AND le.event_type = 'LECTURE_COMPLETE'
                """).setParameter("courseId", courseId).getSingleResult());
    }

    /** 강의별 참여도(입장/완료/고유 시청자/평균 멈춤 위치). 챕터·강의 순서대로. */
    public List<SellerCourseDetailResponse.LectureEngagement> lectureEngagement(Long courseId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT l.id, ch.title AS chapterTitle, l.title, l.duration_seconds,
                  (SELECT COUNT(*) FROM learning_events le WHERE le.lecture_id=l.id AND le.event_type='LECTURE_ENTER') AS enterCount,
                  (SELECT COUNT(*) FROM learning_events le WHERE le.lecture_id=l.id AND le.event_type='LECTURE_COMPLETE') AS completeCount,
                  (SELECT COUNT(DISTINCT le.user_id) FROM learning_events le WHERE le.lecture_id=l.id) AS viewerCount,
                  (SELECT COALESCE(AVG(le.position_seconds),0) FROM learning_events le WHERE le.lecture_id=l.id AND le.event_type='VIDEO_PAUSE') AS avgWatch
                FROM lectures l
                JOIN chapters ch ON l.chapter_id = ch.id
                WHERE ch.course_id = :courseId AND l.deleted_at IS NULL
                ORDER BY ch.order_no, l.order_no
                """).setParameter("courseId", courseId).getResultList();

        List<SellerCourseDetailResponse.LectureEngagement> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new SellerCourseDetailResponse.LectureEngagement(
                    toLong(r[0]), String.valueOf(r[1]), String.valueOf(r[2]), ((Number) r[3]).intValue(),
                    toLong(r[4]), toLong(r[5]), toLong(r[6]), toLong(r[7])));
        }
        return result;
    }

    /**
     * 강의의 멈춤 이벤트(VIDEO_PAUSE) 위치를 반환. 행: [userId, positionSeconds].
     * 시작/재개(VIDEO_START) 수집을 없앴으므로 페어링 없이 멈춘 위치만 버킷팅한다.
     */
    public List<Object[]> pauseEvents(Long lectureId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT le.user_id, le.position_seconds
                FROM learning_events le
                WHERE le.lecture_id = :lectureId
                  AND le.event_type = 'VIDEO_PAUSE'
                  AND le.position_seconds IS NOT NULL
                LIMIT :limit
                """)
                .setParameter("lectureId", lectureId)
                .setParameter("limit", limit)
                .getResultList();
        return rows;
    }

    /** 강좌별 판매 내역(전체 기간 누적): 수강생·판매 건수·매출. 매출 내림차순. */
    public List<SellerAnalyticsResponse.CourseBreakdownRow> courseBreakdown(Long sellerId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT c.id, c.title, c.status, c.price,
                  (SELECT COUNT(*) FROM enrollments e
                     WHERE e.course_id = c.id AND e.status = 'ACTIVE') AS students,
                  (SELECT COUNT(*) FROM order_items oi
                     JOIN orders o ON oi.order_id = o.id
                     WHERE oi.course_id = c.id AND o.status = 'PAID') AS orders,
                  (SELECT COALESCE(SUM(oi.final_price), 0) FROM order_items oi
                     JOIN orders o ON oi.order_id = o.id
                     WHERE oi.course_id = c.id AND o.status = 'PAID') AS revenue
                FROM courses c
                WHERE c.instructor_id = :sellerId
                  AND c.deleted_at IS NULL
                ORDER BY revenue DESC, students DESC, c.id DESC
                """).setParameter("sellerId", sellerId).getResultList();

        List<SellerAnalyticsResponse.CourseBreakdownRow> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new SellerAnalyticsResponse.CourseBreakdownRow(
                    toLong(r[0]), String.valueOf(r[1]), String.valueOf(r[2]), ((Number) r[3]).intValue(),
                    toLong(r[4]), toLong(r[5]), toLong(r[6])));
        }
        return result;
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        return ((Number) o).longValue();
    }
}
