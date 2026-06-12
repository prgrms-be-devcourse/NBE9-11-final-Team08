package com.team08.backend.domain.aifeedback.controller;

import com.team08.backend.domain.aifeedback.dto.AiFeedbackResponse;
import com.team08.backend.domain.aifeedback.service.AiFeedbackService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studies/{studyId}/activities/{activityId}/ai-feedback")
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;

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
