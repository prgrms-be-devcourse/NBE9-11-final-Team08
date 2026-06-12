package com.team08.backend.domain.issuedcoupon.controller;

import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "쿠폰 발급 API", description = "사용자용 쿠폰 다운로드 및 발급 관련 API")
public class CouponController {

    private final IssuedCouponService issuedCouponService;

    // [사용자] 일반 쿠폰 다운로드 /api/coupons/{policyId}/download
    @PostMapping("/{policyId}/download")
    @Operation(summary = "일반 쿠폰 다운로드", description = "지정한 쿠폰 정책 ID로 쿠폰을 다운로드합니다.")
    public IssuedCouponResponse downloadCoupon(
            @Parameter(description = "다운로드할 쿠폰 정책 ID")
            @PathVariable Long policyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return issuedCouponService.downloadCoupon(loginUserPrincipal.user().id(), policyId);
    }
}
