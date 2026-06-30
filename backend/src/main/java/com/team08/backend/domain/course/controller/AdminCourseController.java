package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseRejectRequest;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@Tag(name = "관리자 강좌 심사 API", description = "관리자용 강좌 목록 조회, 상세 미리보기, 심사 승인·반려, 판매 중지, 삭제 API")
public class AdminCourseController {

    private final CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "관리자 강좌 목록 조회", description = "관리자가 전체 강좌 목록을 조회합니다. 상태값을 지정하면 DRAFT, IN_REVIEW, ON_SALE, SUSPENDED 등 특정 상태의 강좌만 조회합니다.")
    public Page<CourseCardResponse> getCoursesForAdmin(
            @Parameter(description = "강좌 상태 필터", example = "IN_REVIEW")
            @RequestParam(name = "status", required = false) CourseStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return courseService.getCoursesForAdmin(status, pageable);
    }

    @GetMapping("/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "관리자 강좌 상세 미리보기", description = "관리자가 강좌 상태와 관계없이 강좌 상세 정보와 커리큘럼을 조회합니다. 관리자 미리보기 화면에서 모든 영상 접근 여부를 확인할 때 사용합니다.")
    public CourseDetailResponse getCourseDetailForAdmin(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId) {
        return courseService.getCourseDetailForAdmin(courseId);
    }

    @PostMapping("/{courseId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 심사 승인", description = "관리자가 IN_REVIEW 상태의 강좌를 승인하여 판매 중(ON_SALE) 상태로 전환합니다.")
    public void approveCourseReview(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.approveCourseReview(courseId, loginUserPrincipal.user().id());
    }

    @PostMapping("/{courseId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 심사 반려", description = "관리자가 IN_REVIEW 상태의 강좌를 반려하고 DRAFT 상태로 되돌립니다. 반려 사유는 강사가 수정 후 재신청할 때 확인할 수 있습니다.")
    public void rejectCourseReview(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseRejectRequest request) {
        courseService.rejectCourseReview(courseId, loginUserPrincipal.user().id(), request.reason());
    }

    @PostMapping("/{courseId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 판매 중지", description = "관리자가 판매 중인 강좌를 판매 중지(SUSPENDED) 상태로 전환합니다. 판매 중지 사유는 강사 관리 화면에서 확인할 수 있습니다.")
    public void suspendCourseByAdmin(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody CourseRejectRequest request) {
        courseService.suspendCourseByAdmin(courseId, loginUserPrincipal.user().id(), request.reason());
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "관리자 강좌 삭제", description = "관리자가 강좌를 삭제합니다. 수강생이 존재하는 강좌는 삭제할 수 없습니다.")
    public void deleteCourseByAdmin(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.deleteCourseByAdmin(courseId, loginUserPrincipal.user().id());
    }
}
