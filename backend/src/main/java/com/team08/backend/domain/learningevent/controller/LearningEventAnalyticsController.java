package com.team08.backend.domain.learningevent.controller;

import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.service.LearningEventAnalyticsService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민/판매자용 대용량 조회·집계 엔드포인트.
 * <p>
 * 서비스 트래픽의 핫 경로(이벤트 적재)인 {@link LearningEventController} 와 클래스 단위로 분리한다.
 * 무거운 운영 조회를 적재 컨트롤러·서비스와 갈라 두면, 이후 별도 인스턴스/커넥션 풀로의
 * 물리 분리가 리팩터링이 아니라 라우팅·배포 변경으로 끝난다. URL 경로는 분리 전과 동일하게
 * 유지해 클라이언트·보안 설정에는 영향이 없다.
 */
@Tag(name = "학습 이벤트 - 운영 조회", description = "관리자/판매자용 학습 이벤트 대용량 조회·통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning-events")
public class LearningEventAnalyticsController {

    private final LearningEventAnalyticsService learningEventAnalyticsService;

    // ── 강의별 학습 통계 조회 ────────────────────────────────────────────────────────
    @Operation(summary = "강의별 학습 통계",
               description = "강좌 단위의 입장 수, 시청 시간, 수강 완료 수 등을 조회합니다. 관리자 또는 강좌 소유 판매자만 접근 가능합니다.")
    @GetMapping("/courses/{courseId}/stats")
    public CourseStatsResponse getCourseStats(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventAnalyticsService.getCourseStats(
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
        return learningEventAnalyticsService.getChapterStats(
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
        return learningEventAnalyticsService.getAllEvents(principal.user().role(), pageable);
    }

    // ── 판매자 강좌 필터링 조회 ────────────────────────────────────────────────
    @Operation(summary = "[판매자] 내 강좌 학습 이벤트 조회",
               description = "판매자 본인이 개설한 강좌의 학습 이벤트만 조회합니다.")
    @GetMapping("/seller")
    public Page<LearningEventResponse> getSellerEvents(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventAnalyticsService.getSellerEvents(
                principal.user().id(), principal.user().role(), pageable
        );
    }
}
