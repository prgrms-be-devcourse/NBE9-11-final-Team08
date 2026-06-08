package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.CurriculumSaveRequest;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
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
    public ResponseEntity<Long> createCourse(
            @Valid @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        Long courseId = courseService.createCourse(request, principal.user().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(courseId);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        courseService.updateCourse(courseId, request, principal.user().id());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        courseService.deleteCourse(courseId, principal.user().id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/curriculum")
    public ResponseEntity<Void> saveCurriculum(
            @PathVariable Long courseId,
            @RequestBody CurriculumSaveRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        courseService.saveCurriculum(courseId, request, principal.user().id());
        return ResponseEntity.ok().build();
    }
}