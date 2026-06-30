package com.team08.backend.domain.couponissuerequest.controller;

import com.team08.backend.domain.couponissuerequest.dto.CouponIssueAllUsersRequest;
import com.team08.backend.domain.couponissuerequest.dto.CouponIssueInactiveUsersRequest;
import com.team08.backend.domain.couponissuerequest.dto.CouponIssueRequestResponse;
import com.team08.backend.domain.couponissuerequest.dto.CouponIssueUsersRequest;
import com.team08.backend.domain.couponissuerequest.service.CouponIssueRequestService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "관리자 쿠폰 발급 요청 API", description = "관리자용 대량 쿠폰 발급 요청 생성 및 처리 상태 조회 API")
public class CouponIssueRequestController {

    private final CouponIssueRequestService couponIssueRequestService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issue-requests")
    @Operation(summary = "쿠폰 발급 요청 목록 조회", description = "관리자가 생성한 대량 쿠폰 발급 요청 목록과 처리 상태를 페이징으로 조회합니다.")
    public Page<CouponIssueRequestResponse> getIssueRequests(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return couponIssueRequestService.getIssueRequests(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issue-requests/{requestId}")
    @Operation(summary = "쿠폰 발급 요청 상세 조회", description = "특정 쿠폰 발급 요청의 대상, 처리 상태, 성공·실패 건수를 조회합니다.")
    public CouponIssueRequestResponse getIssueRequest(
            @Parameter(description = "쿠폰 발급 요청 ID", example = "1") @PathVariable Long requestId
    ) {
        return couponIssueRequestService.getIssueRequest(requestId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{policyId}/issue/users")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "지정 사용자 쿠폰 발급 요청", description = "관리자가 특정 사용자 ID 목록을 대상으로 쿠폰 발급을 비동기로 요청합니다. requestKey로 중복 요청을 방지합니다.")
    public CouponIssueRequestResponse issueToUsers(
            @Parameter(description = "쿠폰 정책 ID", example = "1") @PathVariable Long policyId,
            @Valid @RequestBody CouponIssueUsersRequest request,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return couponIssueRequestService.requestUsersIssue(
                policyId,
                request.userIds(),
                request.requestKey(),
                loginUserPrincipal.user().id()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{policyId}/issue/all")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "전체 사용자 쿠폰 발급 요청", description = "관리자가 전체 사용자를 대상으로 쿠폰 발급을 비동기로 요청합니다. requestKey로 중복 요청을 방지합니다.")
    public CouponIssueRequestResponse issueToAllUsers(
            @Parameter(description = "쿠폰 정책 ID", example = "1") @PathVariable Long policyId,
            @Valid @RequestBody CouponIssueAllUsersRequest request,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return couponIssueRequestService.requestAllUsersIssue(
                policyId,
                request.requestKey(),
                loginUserPrincipal.user().id()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{policyId}/issue/inactive")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "휴면 사용자 쿠폰 발급 요청", description = "관리자가 지정한 미접속 기간 조건에 해당하는 휴면 사용자를 대상으로 쿠폰 발급을 비동기로 요청합니다.")
    public CouponIssueRequestResponse issueToInactiveUsers(
            @Parameter(description = "쿠폰 정책 ID", example = "1") @PathVariable Long policyId,
            @Valid @RequestBody CouponIssueInactiveUsersRequest request,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return couponIssueRequestService.requestInactiveUsersIssue(
                policyId,
                request.inactiveDays(),
                request.maxInactiveDays(),
                request.requestKey(),
                loginUserPrincipal.user().id()
        );
    }
}
