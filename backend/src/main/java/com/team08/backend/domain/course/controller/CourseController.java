package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "강좌 API", description = "공개 강좌 조회와 강사용 강좌 생성·수정·심사 요청·판매 종료·삭제 API")
public class CourseController {

    private final CourseService courseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "강좌 생성", description = "현재 로그인한 강사가 새 강좌를 임시 저장 상태(DRAFT)로 생성합니다. 요청 본문은 강좌 정보 JSON과 선택 썸네일 파일을 multipart/form-data로 전송합니다.")
    public Long createCourse(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestPart("request") CourseCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
        return courseService.createCourse(loginUserPrincipal.user().id(), request, thumbnailFile);
    }

    @GetMapping("/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements
    @Operation(summary = "공개 강좌 상세 조회", description = "판매 중(ON_SALE) 상태인 강좌의 상세 정보와 커리큘럼을 조회합니다. 조회수는 Redis에 누적 후 주기적으로 DB에 반영됩니다.")
    public CourseDetailResponse getCourseDetail(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            HttpServletRequest request) {
        String userIdentifier = (loginUserPrincipal != null)
                ? "MEMBER:" + loginUserPrincipal.user().id()
                : "GUEST:" + getClientIp(request);
        return courseService.getCourseDetail(courseId, userIdentifier);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements
    @Operation(summary = "공개 강좌 목록 조회", description = "판매 중(ON_SALE) 상태인 강좌 목록을 조회합니다. 정렬은 조회수순, 최신순, 가격 낮은순을 지원합니다.")
    public Page<CourseCardResponse> getCourses(
            @Parameter(description = "정렬 기준", example = "VIEW_DESC")
            @RequestParam(name = "sort", defaultValue = "VIEW_DESC") CourseSortType sortType,
            @PageableDefault(size = 10) Pageable pageable) {
        return courseService.getCourses(sortType, pageable);
    }

    @GetMapping("/instructor")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "내 강좌 목록 조회", description = "현재 로그인한 강사가 등록한 모든 강좌를 조회합니다. 상태값을 지정하면 해당 상태의 강좌만 필터링합니다.")
    public Page<CourseCardResponse> getCoursesByInstructor(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Parameter(description = "강좌 상태 필터", example = "DRAFT")
            @RequestParam(name = "status", required = false) CourseStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return courseService.getCoursesByInstructor(loginUserPrincipal.user().id(), status, pageable);
    }

    @GetMapping("/instructor/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "내 강좌 상세 조회", description = "현재 로그인한 강사가 본인이 등록한 강좌의 상세 정보와 상태 사유를 조회합니다. DRAFT, IN_REVIEW, SUSPENDED 상태도 조회할 수 있습니다.")
    public CourseDetailResponse getCourseDetailForInstructor(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return courseService.getCourseDetailForInstructor(courseId, loginUserPrincipal.user().id());
    }

    @PutMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 기본 정보 및 커리큘럼 수정", description = "현재 로그인한 강사가 본인 강좌의 기본 정보, 썸네일, 챕터, 강의 구성을 수정합니다. 판매 중이거나 판매 중지된 강좌는 수정할 수 없습니다.")
    public void updateCourseGeneralInfo(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestPart("request") CourseUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
        courseService.updateCourseGeneralInfo(courseId, loginUserPrincipal.user().id(), request, thumbnailFile);
    }

    @PostMapping("/{courseId}/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 심사 요청", description = "DRAFT 상태의 강좌를 관리자 심사 대기(IN_REVIEW) 상태로 전환합니다. 커리큘럼이 비어 있으면 요청할 수 없습니다.")
    public void requestCourseReview(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.requestCourseReview(courseId, loginUserPrincipal.user().id());
    }

    @DeleteMapping("/{courseId}/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 심사 요청 취소", description = "IN_REVIEW 상태의 강좌 심사 요청을 취소하고 DRAFT 상태로 되돌립니다.")
    public void cancelCourseReview(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.cancelCourseReview(courseId, loginUserPrincipal.user().id());
    }

    @PostMapping("/{courseId}/closing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "강좌 판매 종료", description = "현재 로그인한 강사가 판매 중인 본인 강좌를 판매 중지(SUSPENDED) 상태로 전환합니다.")
    public void closeCourse(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.closeCourse(courseId, loginUserPrincipal.user().id());
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "내 강좌 삭제", description = "현재 로그인한 강사가 본인 강좌를 삭제합니다. 수강생이 존재하는 강좌는 삭제할 수 없습니다.")
    public void deleteCourseByInstructor(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        courseService.deleteCourseByInstructor(courseId, loginUserPrincipal.user().id());
    }

    @PostMapping(value = "/lectures/{lectureId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "강의 영상 업로드 및 인코딩 요청", description = "현재 로그인한 강사가 본인 강의에 영상을 업로드하고 비동기 인코딩을 요청합니다.")
    public void uploadAndEncodeLectureVideo(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Parameter(description = "강의 ID", example = "1") @PathVariable("lectureId") Long lectureId,
            @RequestPart("file") MultipartFile file) {
        courseService.uploadAndEncodeLectureVideo(loginUserPrincipal.user().id(), lectureId, file);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
