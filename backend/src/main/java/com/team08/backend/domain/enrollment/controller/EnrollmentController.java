package com.team08.backend.domain.enrollment.controller;

import com.team08.backend.domain.enrollment.dto.CourseAccessResponse;
import com.team08.backend.domain.enrollment.dto.EnrollmentResponse;
import com.team08.backend.domain.enrollment.service.EnrollmentService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public List<EnrollmentResponse> getMyEnrollments(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return enrollmentService.getMyEnrollments(principal.user().id());
    }

    @GetMapping("/courses/{courseId}/access")
    public CourseAccessResponse canAccess(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long courseId
    ) {
        return enrollmentService.canAccess(principal.user().id(), courseId);
    }
}
