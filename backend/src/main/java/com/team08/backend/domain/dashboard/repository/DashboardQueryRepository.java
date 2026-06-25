package com.team08.backend.domain.dashboard.repository;

import com.team08.backend.domain.dashboard.dto.AnomalyResponse;
import com.team08.backend.domain.dashboard.dto.CourseStatRow;
import com.team08.backend.domain.dashboard.dto.DailySessionPoint;
import com.team08.backend.domain.dashboard.dto.LectureStatRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 대시보드 전용 집계 쿼리 모음.
 * 여러 도메인 테이블을 가로지르는 읽기 전용 네이티브 집계라 EntityManager 로 직접 작성한다.
 * (기존 LearningEventRepository 의 nativeQuery 스타일을 따른다.)
 */
@Repository
public class DashboardQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // ── Overview: 오늘 세션/학습자, 누적 완강 ───────────────────────────
    public long[] overviewToday() {
        Object[] r = (Object[]) em.createNativeQuery("""
                SELECT
                  (SELECT COUNT(*) FROM learning_events
                     WHERE event_type='LECTURE_ENTER' AND DATE(event_time)=CURRENT_DATE) AS todaySessions,
                  (SELECT COUNT(DISTINCT user_id) FROM learning_events
                     WHERE DATE(event_time)=CURRENT_DATE) AS todayLearners,
                  (SELECT COUNT(*) FROM learning_events
                     WHERE event_type='LECTURE_COMPLETE') AS totalCompletions
                """).getSingleResult();
        return new long[]{toLong(r[0]), toLong(r[1]), toLong(r[2])};
    }

    // ── Overview: 일별 세션 추이 ────────────────────────────────────────
    public List<DailySessionPoint> dailySessions(LocalDateTime from, LocalDateTime to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT DATE(event_time) AS dt,
                       SUM(CASE WHEN event_type='LECTURE_ENTER' THEN 1 ELSE 0 END) AS sessions,
                       COUNT(DISTINCT user_id) AS learners
                FROM learning_events
                WHERE event_time >= :from AND event_time < :to
                GROUP BY DATE(event_time)
                ORDER BY dt
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<DailySessionPoint> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new DailySessionPoint(String.valueOf(r[0]), toLong(r[1]), toLong(r[2])));
        }
        return result;
    }

    // ── 드릴다운 1: 강좌별 집계 (dropoutRate 포함, 단일 쿼리) ────────────────
    // 이탈률까지 한 번의 쿼리에서 계산한다. 강좌별로 별도 쿼리를 돌리던 N+1
    // (강좌당 totalLectures + countFullyCompletedEnrollees 2회)을 제거했다.
    // fullyCompleted = ACTIVE 수강자 중 (완료 강의 수 ≥ 전체 강의 수)인 인원.
    public List<CourseStatRow> courseBaseRows(String status, int limit, int offset) {
        StringBuilder inner = new StringBuilder("""
                SELECT c.id AS id, c.title AS title, c.instructor_id AS instructorId, c.status AS status,
                  (SELECT COUNT(*) FROM enrollments e WHERE e.course_id=c.id AND e.status='ACTIVE') AS enrollees,
                  (SELECT COUNT(*) FROM learning_events le WHERE le.course_id=c.id AND le.event_type='LECTURE_ENTER') AS enterCount,
                  (SELECT COUNT(*) FROM learning_events le WHERE le.course_id=c.id AND le.event_type='LECTURE_COMPLETE') AS completionCount,
                  (SELECT COUNT(*) FROM enrollments en
                     WHERE en.course_id=c.id AND en.status='ACTIVE'
                       AND (SELECT COUNT(*) FROM lecture_progresses lp
                              JOIN lectures l ON lp.lecture_id=l.id
                              JOIN chapters ch ON l.chapter_id=ch.id
                            WHERE ch.course_id=c.id AND lp.user_id=en.user_id
                              AND lp.completed=true AND l.deleted_at IS NULL)
                           >= (SELECT COUNT(*) FROM lectures l2
                                 JOIN chapters ch2 ON l2.chapter_id=ch2.id
                               WHERE ch2.course_id=c.id AND l2.deleted_at IS NULL)
                  ) AS fullyCompleted
                FROM courses c
                WHERE c.deleted_at IS NULL
                """);
        if (status != null) {
            inner.append(" AND c.status = :status ");
        }
        inner.append(" ORDER BY enrollees DESC, c.id DESC LIMIT :limit OFFSET :offset ");

        String sql = "SELECT id, title, instructorId, status, enrollees, enterCount, completionCount, "
                + " CASE WHEN enrollees = 0 THEN 0 "
                + "      ELSE ROUND((enrollees - fullyCompleted) * 100.0 / enrollees, 1) END AS dropoutRate "
                + " FROM ( " + inner + " ) sub ORDER BY enrollees DESC, id DESC";

        Query q = em.createNativeQuery(sql)
                .setParameter("limit", limit)
                .setParameter("offset", offset);
        if (status != null) {
            q.setParameter("status", status);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<CourseStatRow> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new CourseStatRow(
                    toLong(r[0]), String.valueOf(r[1]), toLong(r[2]), String.valueOf(r[3]),
                    toLong(r[4]), toLong(r[5]), toLong(r[6]), ((Number) r[7]).doubleValue()));
        }
        return result;
    }

    public long countCourses(String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM courses c WHERE c.deleted_at IS NULL");
        if (status != null) {
            sql.append(" AND c.status = :status");
        }
        Query q = em.createNativeQuery(sql.toString());
        if (status != null) {
            q.setParameter("status", status);
        }
        return toLong(q.getSingleResult());
    }

    /** 강좌의 전체(미삭제) 강의 수 */
    public long totalLectures(Long courseId) {
        return toLong(em.createNativeQuery("""
                SELECT COUNT(*) FROM lectures l
                JOIN chapters c ON l.chapter_id = c.id
                WHERE c.course_id = :courseId AND l.deleted_at IS NULL
                """).setParameter("courseId", courseId).getSingleResult());
    }

    // ── 드릴다운 2: 강의별 집계 ─────────────────────────────────────────
    public List<LectureStatRow> lectureStats(Long courseId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT l.id, ch.title AS chapterTitle, l.title,
                  (SELECT COUNT(*) FROM learning_events le WHERE le.lecture_id=l.id AND le.event_type='LECTURE_ENTER') AS enterCount,
                  (SELECT COUNT(*) FROM learning_events le WHERE le.lecture_id=l.id AND le.event_type='LECTURE_COMPLETE') AS completeCount,
                  (SELECT COALESCE(AVG(le.position_seconds),0) FROM learning_events le WHERE le.lecture_id=l.id AND le.event_type='VIDEO_END') AS avgWatch
                FROM lectures l
                JOIN chapters ch ON l.chapter_id = ch.id
                WHERE ch.course_id = :courseId AND l.deleted_at IS NULL
                ORDER BY ch.order_no, l.order_no
                """).setParameter("courseId", courseId).getResultList();

        List<LectureStatRow> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new LectureStatRow(
                    toLong(r[0]), String.valueOf(r[1]), String.valueOf(r[2]),
                    toLong(r[3]), toLong(r[4]), toLong(r[5])));
        }
        return result;
    }

    // ── 드릴다운 3: 수강자별 진행 기본 행 (totalLectures/progressRate 는 서비스) ──
    public List<Object[]> enrolleeBaseRows(Long courseId, int limit, int offset) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT u.id, u.nickname,
                  (SELECT COUNT(*) FROM lecture_progresses lp
                     JOIN lectures l ON lp.lecture_id = l.id
                     JOIN chapters c ON l.chapter_id = c.id
                   WHERE c.course_id=:courseId AND lp.user_id=u.id AND lp.completed=true AND l.deleted_at IS NULL) AS completedLectures,
                  (SELECT MAX(le.event_time) FROM learning_events le WHERE le.course_id=:courseId AND le.user_id=u.id) AS lastEventTime
                FROM enrollments e
                JOIN users u ON e.user_id = u.id
                WHERE e.course_id=:courseId AND e.status='ACTIVE'
                ORDER BY lastEventTime IS NULL, lastEventTime DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("courseId", courseId)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
        return rows;
    }

    // ── 이상 탐지: 중복 이벤트 다발 ─────────────────────────────────────
    public List<AnomalyResponse.DuplicateBurst> duplicateBursts(int burstThreshold, int windowMinutes) {
        long windowSeconds = (long) windowMinutes * 60;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT user_id, lecture_id, event_type,
                  DATE_FORMAT(FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(event_time)/:win)*:win), '%Y-%m-%d %H:%i') AS bucket,
                  COUNT(*) AS cnt
                FROM learning_events
                WHERE lecture_id IS NOT NULL
                GROUP BY user_id, lecture_id, event_type, bucket
                HAVING cnt > :threshold
                ORDER BY cnt DESC
                LIMIT 100
                """)
                .setParameter("win", windowSeconds)
                .setParameter("threshold", burstThreshold)
                .getResultList();

        List<AnomalyResponse.DuplicateBurst> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new AnomalyResponse.DuplicateBurst(
                    toLong(r[0]), toLong(r[1]), String.valueOf(r[2]), String.valueOf(r[3]), toLong(r[4])));
        }
        return result;
    }

    // ── 감사: 보존 로그 요약 ────────────────────────────────────────────
    public Object[] retentionSummary() {
        return (Object[]) em.createNativeQuery("""
                SELECT COUNT(*) AS cnt, MIN(event_time) AS oldest, MAX(event_time) AS newest
                FROM learning_events
                """).getSingleResult();
    }

    // ── 감사: 접근/변경 이력 (course_status_histories + 최근 학습 이벤트) ──
    @SuppressWarnings("unchecked")
    public List<Object[]> recentStatusHistory(int limit) {
        return em.createNativeQuery("""
                SELECT course_id, from_status, to_status, changed_by, created_at
                FROM course_status_histories
                ORDER BY created_at DESC
                LIMIT :limit
                """).setParameter("limit", limit).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> recentLearningEvents(int limit) {
        return em.createNativeQuery("""
                SELECT user_id, event_type, lecture_id, event_time
                FROM learning_events
                ORDER BY event_time DESC
                LIMIT :limit
                """).setParameter("limit", limit).getResultList();
    }

    // ── 감사: 정합성 오류(orphan) 탐지 ─────────────────────────────────
    /** 존재하지 않는 강좌를 참조하는 학습 이벤트 */
    @SuppressWarnings("unchecked")
    public List<Long> orphanLearningEventIds(int limit) {
        return toLongList(em.createNativeQuery("""
                SELECT le.id FROM learning_events le
                LEFT JOIN courses c ON le.course_id = c.id
                WHERE le.course_id IS NOT NULL AND c.id IS NULL
                LIMIT :limit
                """).setParameter("limit", limit).getResultList());
    }

    /** 존재하지 않는 강의를 참조하는 진도 기록 */
    @SuppressWarnings("unchecked")
    public List<Long> orphanLectureProgressIds(int limit) {
        return toLongList(em.createNativeQuery("""
                SELECT lp.id FROM lecture_progresses lp
                LEFT JOIN lectures l ON lp.lecture_id = l.id
                WHERE l.id IS NULL
                LIMIT :limit
                """).setParameter("limit", limit).getResultList());
    }

    /** 완료(completed=true) 표시인데 LECTURE_COMPLETE 이벤트가 없는 진도 기록 */
    @SuppressWarnings("unchecked")
    public List<Long> completedWithoutEventIds(int limit) {
        return toLongList(em.createNativeQuery("""
                SELECT lp.id FROM lecture_progresses lp
                WHERE lp.completed = true
                  AND NOT EXISTS (
                    SELECT 1 FROM learning_events le
                    WHERE le.user_id = lp.user_id AND le.lecture_id = lp.lecture_id
                      AND le.event_type = 'LECTURE_COMPLETE')
                LIMIT :limit
                """).setParameter("limit", limit).getResultList());
    }

    // ── 변환 헬퍼 ───────────────────────────────────────────────────────
    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        return ((Number) o).longValue();
    }

    public static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (o instanceof LocalDateTime ldt) {
            return ldt;
        }
        return null;
    }

    private static List<Long> toLongList(List<?> raw) {
        List<Long> ids = new ArrayList<>();
        for (Object o : raw) {
            ids.add(toLong(o));
        }
        return ids;
    }
}
