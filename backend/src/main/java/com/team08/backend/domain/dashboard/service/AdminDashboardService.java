package com.team08.backend.domain.dashboard.service;

import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.dashboard.dto.AnomalyResponse;
import com.team08.backend.domain.dashboard.dto.AuditResponse;
import com.team08.backend.domain.dashboard.dto.CourseStatRow;
import com.team08.backend.domain.dashboard.dto.DailySessionPoint;
import com.team08.backend.domain.dashboard.dto.EnrolleeRow;
import com.team08.backend.domain.dashboard.dto.LectureStatRow;
import com.team08.backend.domain.dashboard.dto.OverviewResponse;
import com.team08.backend.domain.dashboard.exception.DashboardException;
import com.team08.backend.domain.dashboard.repository.DashboardQueryRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int INTEGRITY_SCAN_LIMIT = 201; // 200건 초과 여부 판별용
    private static final int INTEGRITY_SAMPLE_LIMIT = 5;
    private static final int HIGH_INCOMPLETION_SCAN_LIMIT = 200;

    private final DashboardQueryRepository dashboardQueryRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningEventRepository learningEventRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;

    // ── ① Overview ──────────────────────────────────────────────────────
    public OverviewResponse getOverview(String requesterRole) {
        requireAdmin(requesterRole);

        long[] today = dashboardQueryRepository.overviewToday();
        return new OverviewResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.ROLE_SELLER),
                userRepository.countByRole(UserRole.ROLE_USER),
                courseRepository.countByStatus(CourseStatus.ON_SALE),
                enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE),
                learningEventRepository.count(),
                today[2],   // totalCompletions
                today[0],   // todaySessions
                today[1]    // todayActiveLearners
        );
    }

    public List<DailySessionPoint> getDailySessions(String requesterRole, LocalDate from, LocalDate to) {
        requireAdmin(requesterRole);

        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        // [start 00:00, end+1 00:00)
        return dashboardQueryRepository.dailySessions(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    // ── ② 드릴다운 ──────────────────────────────────────────────────────
    public Page<CourseStatRow> getCourseStats(String requesterRole, String status, int page, int size) {
        requireAdmin(requesterRole);

        int offset = page * size;
        List<CourseStatRow> rows = dashboardQueryRepository.courseBaseRows(status, size, offset);
        long total = dashboardQueryRepository.countCourses(status);

        return new PageImpl<>(rows, PageRequest.of(page, size), total);
    }

    public List<LectureStatRow> getLectureStats(String requesterRole, Long courseId) {
        requireAdmin(requesterRole);
        return dashboardQueryRepository.lectureStats(courseId);
    }

    public Page<EnrolleeRow> getEnrollees(String requesterRole, Long courseId, int page, int size) {
        requireAdmin(requesterRole);

        int offset = page * size;
        long totalLectures = dashboardQueryRepository.totalLectures(courseId);
        long totalEnrollees = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);

        List<EnrolleeRow> rows = new ArrayList<>();
        for (Object[] r : dashboardQueryRepository.enrolleeBaseRows(courseId, size, offset)) {
            Long userId = ((Number) r[0]).longValue();
            String nickname = String.valueOf(r[1]);
            long completed = ((Number) r[2]).longValue();
            LocalDateTime lastEventTime = DashboardQueryRepository.toLocalDateTime(r[3]);
            double progressRate = (totalLectures > 0)
                    ? round1(completed * 100.0 / totalLectures) : 0.0;
            rows.add(new EnrolleeRow(userId, nickname, completed, totalLectures, progressRate, lastEventTime));
        }
        return new PageImpl<>(rows, PageRequest.of(page, size), totalEnrollees);
    }

    public Page<LearningEventResponse> getUserTimeline(String requesterRole, Long userId, Long courseId, Pageable pageable) {
        requireAdmin(requesterRole);

        Page<LearningEvent> source = (courseId != null)
                ? learningEventRepository.findByUserIdAndCourseId(userId, courseId, pageable)
                : learningEventRepository.findByUserId(userId, pageable);
        return source.map(LearningEventResponse::from);
    }

    // ── ③ 이상 탐지 ─────────────────────────────────────────────────────
    public AnomalyResponse getAnomalies(String requesterRole, Double incompletionThreshold, Integer burstThreshold, Integer windowMinutes) {
        requireAdmin(requesterRole);

        double incompletion = (incompletionThreshold != null) ? incompletionThreshold : 50.0;
        int burst = (burstThreshold != null) ? burstThreshold : 10;
        int window = (windowMinutes != null && windowMinutes > 0) ? windowMinutes : 1;

        List<CourseStatRow> highIncompletion = dashboardQueryRepository.courseBaseRows(null, HIGH_INCOMPLETION_SCAN_LIMIT, 0).stream()
                .filter(r -> r.enrollees() > 0)
                .filter(r -> r.incompletionRate() > incompletion)
                .sorted(Comparator.comparingDouble(CourseStatRow::incompletionRate).reversed())
                .toList();

        List<AnomalyResponse.DuplicateBurst> bursts = dashboardQueryRepository.duplicateBursts(burst, window);

        return new AnomalyResponse(incompletion, burst, window, highIncompletion, bursts);
    }

    // ── ④ 보존 감사 ─────────────────────────────────────────────────────
    public AuditResponse getAudit(String requesterRole) {
        requireAdmin(requesterRole);

        Object[] retentionRow = dashboardQueryRepository.retentionSummary();
        AuditResponse.Retention retention = new AuditResponse.Retention(
                ((Number) retentionRow[0]).longValue(),
                DashboardQueryRepository.toLocalDateTime(retentionRow[1]),
                DashboardQueryRepository.toLocalDateTime(retentionRow[2]),
                courseStatusHistoryRepository.count(),
                lectureProgressRepository.count()
        );

        List<AuditResponse.AccessHistoryEntry> accessHistory = new ArrayList<>();
        for (Object[] r : dashboardQueryRepository.recentStatusHistory(20)) {
            accessHistory.add(new AuditResponse.AccessHistoryEntry(
                    "COURSE_STATUS",
                    "강좌 " + r[0] + ": " + r[1] + " → " + r[2],
                    (r[3] != null) ? ((Number) r[3]).longValue() : null,
                    DashboardQueryRepository.toLocalDateTime(r[4])));
        }
        for (Object[] r : dashboardQueryRepository.recentLearningEvents(20)) {
            accessHistory.add(new AuditResponse.AccessHistoryEntry(
                    "LEARNING_EVENT",
                    r[1] + " (강의 " + r[2] + ")",
                    (r[0] != null) ? ((Number) r[0]).longValue() : null,
                    DashboardQueryRepository.toLocalDateTime(r[3])));
        }
        accessHistory.sort(Comparator.comparing(
                AuditResponse.AccessHistoryEntry::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<AuditResponse.AccessHistoryEntry> trimmed = accessHistory.stream().limit(30).toList();

        List<AuditResponse.IntegrityError> integrityErrors = List.of(
                integrity("ORPHAN_LEARNING_EVENT", "존재하지 않는 강좌를 참조하는 학습 이벤트",
                        dashboardQueryRepository.orphanLearningEventIds(INTEGRITY_SCAN_LIMIT)),
                integrity("ORPHAN_LECTURE_PROGRESS", "존재하지 않는 강의를 참조하는 진도 기록",
                        dashboardQueryRepository.orphanLectureProgressIds(INTEGRITY_SCAN_LIMIT)),
                integrity("COMPLETED_WITHOUT_EVENT", "완료 표시이나 LECTURE_COMPLETE 이벤트가 없는 진도 기록",
                        dashboardQueryRepository.completedWithoutEventIds(INTEGRITY_SCAN_LIMIT))
        );

        return new AuditResponse(retention, trimmed, integrityErrors);
    }

    // ── helpers ─────────────────────────────────────────────────────────
    private AuditResponse.IntegrityError integrity(String type, String description, List<Long> ids) {
        long count = ids.size();
        String desc = (count >= INTEGRITY_SCAN_LIMIT) ? description + " (200건 이상)" : description;
        List<Long> sample = ids.stream().limit(INTEGRITY_SAMPLE_LIMIT).toList();
        return new AuditResponse.IntegrityError(type, desc, count, sample);
    }

    private void requireAdmin(String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            throw new DashboardException(ErrorCode.ADMIN_ACCESS_DENIED);
        }
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
