package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.CurriculumSaveRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
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
    private final UserRepository userRepository;

    /**
     * 강의 상품 등록 API
     * POST /api/courses
     */
    @PostMapping
    public ResponseEntity<Long> createCourse(
            @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        User loginUser = userRepository.findById(principal.user().id())
                .orElseThrow(() -> new IllegalArgumentException("인증된 유저 정보를 찾을 수 없습니다."));

        Long courseId = courseService.createCourse(request, loginUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseId);
    }

    /**
     * 강의 상세 조회 API
     * GET /api/courses/{courseId}
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourse(@PathVariable Long courseId) {
        Course course = courseService.getCourse(courseId);
        return ResponseEntity.ok(CourseDetailResponse.from(course)); // DTO 변환 필요
    }

    /**
     * 강의 상품 수정 API
     * PUT /api/courses/{courseId}
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        User loginUser = userRepository.findById(principal.user().id())
                .orElseThrow(() -> new IllegalArgumentException("인증된 유저 정보를 찾을 수 없습니다."));

        courseService.updateCourse(courseId, request, loginUser);
        return ResponseEntity.ok().build();
    }

    /**
     * 강의 삭제 API
     * DELETE /api/courses/{courseId}
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        User loginUser = userRepository.findById(principal.user().id())
                .orElseThrow(() -> new IllegalArgumentException("유저 정보가 없습니다."));

        courseService.deleteCourse(courseId, loginUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * 강의 커리큘럼(Chapter - Lecture 계층) 일괄 저장 및 수정 API
     * POST /api/courses/{courseId}/curriculum
     */
    @PostMapping("/{courseId}/curriculum")
    public ResponseEntity<Void> saveCurriculum(
            @PathVariable Long courseId,
            @RequestBody CurriculumSaveRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        User loginUser = userRepository.findById(principal.user().id())
                .orElseThrow(() -> new IllegalArgumentException("인증된 유저 정보를 찾을 수 없습니다."));

        courseService.saveCurriculum(courseId, request, loginUser);
        return ResponseEntity.ok().build();
    }
}