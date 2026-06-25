package com.team08.backend.domain.dashboard.controller;

import com.team08.backend.domain.dashboard.dto.AnomalyResponse;
import com.team08.backend.domain.dashboard.dto.AuditResponse;
import com.team08.backend.domain.dashboard.dto.CourseStatRow;
import com.team08.backend.domain.dashboard.dto.DailySessionPoint;
import com.team08.backend.domain.dashboard.dto.EnrolleeRow;
import com.team08.backend.domain.dashboard.dto.LectureStatRow;
import com.team08.backend.domain.dashboard.dto.OverviewResponse;
import com.team08.backend.domain.dashboard.service.AdminDashboardService;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "[관리자] 대시보드", description = "플랫폼 현황·학습활동 드릴다운·이상 탐지·데이터 감사 (관리자 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    // ── ① Overview ──────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "플랫폼 전체 현황 KPI")
    @GetMapping("/overview")
    public OverviewResponse getOverview(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getOverview(principal.user().role());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "일별 세션 추이", description = "기본 최근 30일. from/to(yyyy-MM-dd)로 기간 지정 가능.")
    @GetMapping("/sessions/daily")
    public List<DailySessionPoint> getDailySessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getDailySessions(principal.user().role(), from, to);
    }

    // ── ② 드릴다운 ──────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "강좌별 학습 집계 (드릴다운 1단계)")
    @GetMapping("/courses")
    public Page<CourseStatRow> getCourseStats(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getCourseStats(principal.user().role(), status, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "강의별 학습 집계 (드릴다운 2단계)")
    @GetMapping("/courses/{courseId}/lectures")
    public List<LectureStatRow> getLectureStats(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getLectureStats(principal.user().role(), courseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "수강자별 진행 현황 (드릴다운 3단계)")
    @GetMapping("/courses/{courseId}/enrollees")
    public Page<EnrolleeRow> getEnrollees(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getEnrollees(principal.user().role(), courseId, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "개별 수강자 이벤트 타임라인", description = "courseId 지정 시 해당 강좌로 한정.")
    @GetMapping("/users/{userId}/timeline")
    public Page<LearningEventResponse> getUserTimeline(
            @PathVariable Long userId,
            @RequestParam(required = false) Long courseId,
            @PageableDefault(size = 30) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getUserTimeline(principal.user().role(), userId, courseId, pageable);
    }

    // ── ③ 이상 탐지 ─────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "이상 데이터 탐지", description = "미완강률 임계값 초과 강좌 + 중복 이벤트 다발(분 단위 버킷) 탐지.")
    @GetMapping("/anomalies")
    public AnomalyResponse getAnomalies(
            @RequestParam(required = false) Double incompletionThreshold,
            @RequestParam(required = false) Integer burstThreshold,
            @RequestParam(required = false) Integer windowMinutes,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getAnomalies(principal.user().role(), incompletionThreshold, burstThreshold, windowMinutes);
    }

    // ── ④ 보존 감사 ─────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "학습 데이터 보존 현황 감사", description = "보존 로그·접근 이력·정합성 오류(파생 뷰).")
    @GetMapping("/audit/retention")
    public AuditResponse getAudit(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return adminDashboardService.getAudit(principal.user().role());
    }
}
