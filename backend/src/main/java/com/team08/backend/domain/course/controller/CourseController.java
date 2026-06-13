package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @SecurityRequirements
    public CourseDetailResponse getCourseDetail(@PathVariable("courseId") Long courseId) {
        return courseService.getCourseDetail(courseId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements
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
    public void requestCourseReview(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.requestCourseReview(courseId, loginUserPrincipal.user().id());
    }

    @DeleteMapping("/{courseId}/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelCourseReview(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.cancelCourseReview(courseId, loginUserPrincipal.user().id());
    }

    @PostMapping("/{courseId}/closing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeCourse(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.closeCourse(courseId, loginUserPrincipal.user().id());
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourseByInstructor(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.deleteCourseByInstructor(courseId, loginUserPrincipal.user().id());
    }

    @PostMapping(value = "/lectures/{lectureId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void uploadAndEncodeLectureVideo(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @PathVariable("lectureId") Long lectureId,
            @RequestPart("file") MultipartFile file) {
        courseService.uploadAndEncodeLectureVideo(loginUserPrincipal.user().id(), lectureId, file);
    }
}