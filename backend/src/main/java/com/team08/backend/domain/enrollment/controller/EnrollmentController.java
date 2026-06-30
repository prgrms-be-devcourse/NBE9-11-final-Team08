package com.team08.backend.domain.enrollment.controller;

import com.team08.backend.domain.enrollment.dto.ActiveEnrollmentExistsResponse;
import com.team08.backend.domain.enrollment.dto.EnrolledCourseResponse;
import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "수강 등록 API", description = "강의 수강 등록 상태 조회 관련 API")
public class EnrollmentController {

    private final EnrollmentQueryService enrollmentQueryService;

    @Operation(
            summary = "내 활성 수강 강좌 목록 조회",
            description = "현재 로그인한 사용자의 ACTIVE 수강 등록 강좌를 최신 등록순으로 조회합니다."
    )
    @GetMapping("/me/courses")
    public List<EnrolledCourseResponse> getMyActiveCourses(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return enrollmentQueryService.getMyActiveCourses(loginUserPrincipal.user().id());
    }

    @Operation(
            summary = "강의 활성 수강 여부 조회",
            description = """
                    로그인한 사용자가 해당 강의에 활성(ACTIVE) 상태의 수강 등록을 보유하고 있는지 조회한다.
                    응답의 exists 가 true 면 활성 수강 등록이 존재한다.
                    """)
    @GetMapping("/courses/{courseId}/active")
    public ActiveEnrollmentExistsResponse hasActiveEnrollment(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Parameter(description = "강의(코스) ID") @PathVariable Long courseId
    ) {
        return new ActiveEnrollmentExistsResponse(
                enrollmentQueryService.hasActiveEnrollment(loginUserPrincipal.user().id(), courseId)
        );
    }
}
