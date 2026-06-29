package com.team08.backend.domain.issuedcoupon.controller;

import com.team08.backend.domain.issuedcoupon.dto.CouponDownloadResponse;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponQueryFacade;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicySummaryResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.service.CouponPolicyService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "쿠폰 발급 API", description = "사용자용 쿠폰 다운로드 및 발급 관련 API")
public class IssuedCouponController {

    private final IssuedCouponService issuedCouponService;
    private final IssuedCouponQueryFacade issuedCouponQueryFacade;
    private final CouponPolicyService couponPolicyService;

    // [사용자] 쿠폰 정책 목록 조회 /api/coupons
    @GetMapping
    @Operation(summary = "발급 가능한 쿠폰 정책 목록 조회", description = "모든 사용자가 발급 가능한 쿠폰 정책 목록을 조회합니다.")
    public Page<CouponPolicySummaryResponse> getCoupons(
            @ParameterObject @ModelAttribute CouponPolicySearchRequest condition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return couponPolicyService.getCouponPolicies(condition, pageable);
    }

    // [사용자] 쿠폰 다운로드 /api/coupons/{policyId}/download
    @PostMapping("/{policyId}/download")
    @Operation(summary = "쿠폰 다운로드", description = "지정한 쿠폰 정책 ID로 쿠폰을 다운로드합니다. (일반/선착순 등 타입에 따라 자동 처리)")
    public CouponDownloadResponse downloadCoupon(
            @Parameter(description = "다운로드할 쿠폰 정책 ID")
            @PathVariable Long policyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return issuedCouponService.downloadCoupon(loginUserPrincipal.user().id(), policyId);
    }

    // [사용자] 내 쿠폰 목록 조회 /api/coupons/me
    @GetMapping("/me")
    @Operation(summary = "내 쿠폰 목록 조회", description = "현재 로그인한 사용자가 보유한 모든 쿠폰 목록을 조회합니다.")
    public List<CouponListResponse> getMyCoupons(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return issuedCouponQueryFacade.getMyCoupons(loginUserPrincipal.user().id());
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
