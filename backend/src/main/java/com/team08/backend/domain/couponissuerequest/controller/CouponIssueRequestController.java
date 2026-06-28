package com.team08.backend.domain.couponissuerequest.controller;

import com.team08.backend.domain.couponissuerequest.dto.CouponIssueAllUsersRequest;
import com.team08.backend.domain.couponissuerequest.dto.CouponIssueRequestResponse;
import com.team08.backend.domain.couponissuerequest.dto.CouponIssueUsersRequest;
import com.team08.backend.domain.couponissuerequest.service.CouponIssueRequestService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
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
public class CouponIssueRequestController {

    private final CouponIssueRequestService couponIssueRequestService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issue-requests")
    public Page<CouponIssueRequestResponse> getIssueRequests(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return couponIssueRequestService.getIssueRequests(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issue-requests/{requestId}")
    public CouponIssueRequestResponse getIssueRequest(
            @PathVariable Long requestId
    ) {
        return couponIssueRequestService.getIssueRequest(requestId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{policyId}/issue/users")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CouponIssueRequestResponse issueToUsers(
            @PathVariable Long policyId,
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
    public CouponIssueRequestResponse issueToAllUsers(
            @PathVariable Long policyId,
            @Valid @RequestBody CouponIssueAllUsersRequest request,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return couponIssueRequestService.requestAllUsersIssue(
                policyId,
                request.requestKey(),
                loginUserPrincipal.user().id()
        );
    }
}
