package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import com.team08.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCourse(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseCreateRequest request) {

        Long courseId = courseService.createCourse(loginUserPrincipal.user().id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(courseId));
    }
}