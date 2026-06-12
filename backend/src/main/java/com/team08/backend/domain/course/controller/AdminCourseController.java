package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseRejectRequest;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    @PostMapping("/{courseId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveCourseReview(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.approveCourseReview(courseId, loginUserPrincipal.user().id());
    }

    @PostMapping("/{courseId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectCourseReview(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseRejectRequest request) {
        courseService.rejectCourseReview(courseId, loginUserPrincipal.user().id(), request.reason());
    }

    @PostMapping("/{courseId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendCourseByAdmin(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseRejectRequest request) {
        courseService.suspendCourseByAdmin(courseId, loginUserPrincipal.user().id(), request.reason());
    }
}