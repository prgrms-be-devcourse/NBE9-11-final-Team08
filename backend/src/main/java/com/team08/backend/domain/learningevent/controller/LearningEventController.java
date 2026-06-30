package com.team08.backend.domain.learningevent.controller;

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

// 서비스 트래픽의 핫 경로(이벤트 적재 + 본인 활동 조회)만 담당한다.
// 어드민/판매자용 대용량 조회·통계는 learningevent.analytics 패키지로 분리되어 있다.
@Tag(name = "학습 이벤트", description = "학습 활동 기록 및 본인 활동 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning-events")
public class LearningEventController {

    private final LearningEventService learningEventService;

    // ── 이벤트 기록 ─────────────────────────────────────────────────────
    @Operation(summary = "학습 이벤트 기록",
               description = "강의 입장, 영상 시청, 재생 위치, 수강 완료이벤트를 기록합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public LearningEventResponse recordEvent(
            @Valid @RequestBody RecordLearningEventRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
            // 적재 후 LearningEventRecorded 가 발행되고, 각 반응 리스너가 자기 타입만 처리한다.
            // 예: LECTURE_EXIT 시 ExitProgressFlushListener 가 커밋 후 best-effort 로 위치를 flush.
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
}
