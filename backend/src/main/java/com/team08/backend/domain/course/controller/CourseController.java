package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.*;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long createCourse(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseCreateRequest request) {
        return courseService.createCourse(loginUserPrincipal.user().id(), request);
    }

    @GetMapping("/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    public CourseDetailResponse getCourseDetail(@PathVariable("courseId") Long courseId) {
        return courseService.getCourseDetail(courseId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<CourseCardResponse> getCourses(
            @RequestParam(name = "sort", defaultValue = "VIEW_DESC") CourseSortType sortType,
            @PageableDefault(size = 10) Pageable pageable) {
        return courseService.getCourses(sortType, pageable);
    }

    @PutMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCourseGeneralInfo(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseUpdateRequest request) {
        courseService.updateCourseGeneralInfo(courseId, loginUserPrincipal.user().id(), request);
    }

    @PostMapping("/{courseId}/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitCourseReview(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.submitCourseReview(courseId, loginUserPrincipal.user().id());
    }
}