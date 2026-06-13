package com.team08.backend.domain.aifeedback.controller;

import com.team08.backend.domain.aifeedback.dto.AiFeedbackResponse;
import com.team08.backend.domain.aifeedback.service.AiFeedbackService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "스터디 활동 AI 피드백",
        description = "스터디 활동의 AI 피드백 생성 및 조회 API"
)
@RestController
@RequestMapping("/api/studies/{studyId}/activities/{activityId}/ai-feedback")
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;

    @Operation(
            summary = "AI 피드백 생성",
            description = "활동 작성자가 한국어 AI 피드백을 생성하거나 재요청합니다."
    )
    @PostMapping
    public AiFeedbackResponse generate(
            @PathVariable Long studyId,
            @PathVariable Long activityId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return aiFeedbackService.generate(
                studyId,
                activityId,
                loginUserPrincipal.user().id()
        );
    }

    @Operation(
            summary = "AI 피드백 조회",
            description = "ACTIVE 스터디 멤버가 활동에 저장된 AI 피드백을 조회합니다."
    )
    @GetMapping
    public AiFeedbackResponse get(
            @PathVariable Long studyId,
            @PathVariable Long activityId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return aiFeedbackService.get(
                studyId,
                activityId,
                loginUserPrincipal.user().id()
        );
    }
}
