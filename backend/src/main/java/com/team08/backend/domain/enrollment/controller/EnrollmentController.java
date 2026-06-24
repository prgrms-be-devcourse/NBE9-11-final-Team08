package com.team08.backend.domain.enrollment.controller;

import com.team08.backend.domain.enrollment.dto.ActiveEnrollmentExistsResponse;
import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentQueryService enrollmentQueryService;

    @GetMapping("/courses/{courseId}/active")
    public ActiveEnrollmentExistsResponse hasActiveEnrollment(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @PathVariable Long courseId
    ) {
        return new ActiveEnrollmentExistsResponse(
                enrollmentQueryService.hasActiveEnrollment(loginUserPrincipal.user().id(), courseId)
        );
    }
}
