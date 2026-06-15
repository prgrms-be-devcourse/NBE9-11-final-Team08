package com.team08.backend.domain.learningevent.controller;

import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.service.LearningEventService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "학습 이벤트", description = "학습 활동 기록 및 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning-events")
public class LearningEventController {

    private final LearningEventService learningEventService;

    // ── 이벤트 기록 ─────────────────────────────────────────────────────
    @Operation(summary = "학습 이벤트 기록",
               description = "강의 입장, 영상 시청, 재생 위치, 수강 완료, 회고 작성, QnA 작성 이벤트를 기록합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public LearningEventResponse recordEvent(
            @Valid @RequestBody RecordLearningEventRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.recordEvent(principal.user().id(), request);
    }

    // ── 사용자별 활동 조회 ─────────────────────────────────────────────────
    @Operation(summary = "사용자별 학습 활동 조회",
               description = "본인 또는 관리자가 특정 사용자의 학습 이벤트 이력을 조회합니다.")
    @GetMapping("/users/{userId}/activities")
    public Page<LearningEventResponse> getUserActivities(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getUserActivities(
                principal.user().id(), userId, principal.user().role(), pageable
        );
    }

    // ── 강의별 학습 통계 조회 ────────────────────────────────────────────────────────
    @Operation(summary = "강의별 학습 통계",
               description = "강좌 단위의 입장 수, 시청 시간, 수강 완료 수 등을 조회합니다. 관리자 또는 강좌 소유 판매자만 접근 가능합니다.")
    @GetMapping("/courses/{courseId}/stats")
    public CourseStatsResponse getCourseStats(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getCourseStats(
                principal.user().id(), courseId, principal.user().role()
        );
    }

    // ── 챕터별 학습 통계 조회 ───────────────────────────────────────────────────────
    @Operation(summary = "챕터별 학습 통계",
               description = "챕터 단위의 입장 수, 완료 수, 평균 시청 시간을 조회합니다. 관리자 또는 판매자만 접근 가능합니다.")
    @GetMapping("/chapters/{chapterId}/stats")
    public ChapterStatsResponse getChapterStats(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getChapterStats(
                principal.user().id(), chapterId, principal.user().role()
        );
    }

    // ── 관리자 전체 조회 ──────────────────────────────────────────────────
    @Operation(summary = "[관리자] 전체 학습 이벤트 조회",
               description = "모든 강좌의 학습 이벤트를 페이지네이션으로 조회합니다. 관리자 전용입니다.")
    @GetMapping("/admin")
    public Page<LearningEventResponse> getAllEvents(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getAllEvents(principal.user().role(), pageable);
    }

    // ── 판매자 강좌 필터링 조회 ────────────────────────────────────────────────
    @Operation(summary = "[판매자] 내 강좌 학습 이벤트 조회",
               description = "판매자 본인이 개설한 강좌의 학습 이벤트만 조회합니다.")
    @GetMapping("/seller")
    public Page<LearningEventResponse> getSellerEvents(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getSellerEvents(
                principal.user().id(), principal.user().role(), pageable
        );
    }
}
