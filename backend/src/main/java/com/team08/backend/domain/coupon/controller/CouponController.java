package com.team08.backend.domain.coupon.controller;

import com.team08.backend.domain.coupon.dto.CouponListResponse;
import com.team08.backend.domain.coupon.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.coupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.repository.CouponPolicyRepository;
import com.team08.backend.domain.coupon.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponIssueService couponIssueService;
    private final CouponPolicyRepository couponPolicyRepository;

    // [관리자] 쿠폰 생성 /api/coupons
    @PostMapping
    public ResponseEntity<String> createCoupon(@RequestBody CouponPolicyCreateRequest request) {

        CouponPolicy newCoupon = CouponPolicy.builder()
                .name(request.name())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .validDays(request.validDays())
                .totalQuantity(request.totalQuantity())
                .couponType(request.couponType())
                .couponTarget(request.couponTarget())
                .issueStartDate(request.issueStartDate())
                .issueEndDate(request.issueEndDate())
                .build();

        couponPolicyRepository.save(newCoupon);
        return ResponseEntity.ok("쿠폰이 성공적으로 생성되었습니다.");
    }

    // [사용자] 일반 쿠폰 다운로드 /api/coupons/{policyId}/download
    @PostMapping("/{policyId}/download")
    public ResponseEntity<String> downloadCoupon(
            @PathVariable Long policyId,
            @RequestParam Long userId) {

        couponIssueService.downloadCoupon(userId, policyId);
        return ResponseEntity.ok("쿠폰이 성공적으로 발급되었습니다!");
    }

    // [사용자] 선착순 쿠폰 다운로드 /api/coupons/{policyId}/download-fcfs
    @PostMapping("/{policyId}/download-fcfs")
    public ResponseEntity<String> downloadFcfsCoupon(
            @PathVariable Long policyId,
            @RequestParam Long userId) {

        couponIssueService.downloadFcfsCoupon(userId, policyId);
        return ResponseEntity.ok("선착순 쿠폰이 성공적으로 발급되었습니다!");
    }

    // [사용자] 내 쿠폰 목록 조회 /api/coupons/me
    @GetMapping("/me")
    public ResponseEntity<List<CouponListResponse>> getMyCoupons(@RequestParam Long userId) {

        List<CouponListResponse> myCoupons = couponIssueService.getMyCoupons(userId);
        return ResponseEntity.ok(myCoupons);
    }

    // [사용자] 쿠폰 적용 시 예상 할인 금액 조회 /api/coupons/calculate
    @GetMapping("/calculate")
    public ResponseEntity<ExpectedDiscountResponse> calculateExpectedDiscount(
            @RequestParam Long issuedCouponId,
            @RequestParam Long userId,
            @RequestParam int originalPrice) {

        ExpectedDiscountResponse response = couponIssueService.calculateExpectedDiscount(userId, issuedCouponId, originalPrice);
        return ResponseEntity.ok(response);
    }


}
