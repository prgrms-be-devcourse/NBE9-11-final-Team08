package com.team08.backend.domain.issuedcoupon.controller;

import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // [사용자] 선착순 쿠폰 다운로드 /api/coupons/{policyId}/download-fcfs
    @PostMapping("/{policyId}/download-fcfs")
    @Operation(summary = "선착순 쿠폰 다운로드", description = "재고가 제한된 선착순 쿠폰을 다운로드합니다.")
    public IssuedCouponResponse downloadFcfsCoupon(
            @Parameter(description = "다운로드할 쿠폰 정책 ID")
            @PathVariable Long policyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return issuedCouponService.downloadFcfsCoupon(loginUserPrincipal.user().id(), policyId);
    }

    // [사용자] 내 쿠폰 목록 조회 /api/coupons/me
    @GetMapping("/me")
    @Operation(summary = "내 쿠폰 목록 조회", description = "현재 로그인한 사용자가 보유한 모든 쿠폰 목록을 조회합니다.")
    public List<CouponListResponse> getMyCoupons(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return issuedCouponService.getMyCoupons(loginUserPrincipal.user().id());
    }

    // [사용자] 쿠폰 적용 시 예상 할인 금액 조회 /api/coupons/{issuedCouponId}/discount
    @GetMapping("/{issuedCouponId}/discount")
    @Operation(summary = "예상 할인 금액 조회", description = "결제 전 특정 쿠폰을 적용했을 때의 예상 할인 금액과 최종 가격을 조회합니다.")
    public ExpectedDiscountResponse calculateExpectedDiscount(
            @Parameter(description = "적용할 발급 쿠폰 ID")
            @PathVariable Long issuedCouponId,
            @Parameter(description = "상품 원가")
            @RequestParam int originalPrice,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return issuedCouponService.calculateExpectedDiscount(loginUserPrincipal.user().id(), issuedCouponId, originalPrice);
    }
}
