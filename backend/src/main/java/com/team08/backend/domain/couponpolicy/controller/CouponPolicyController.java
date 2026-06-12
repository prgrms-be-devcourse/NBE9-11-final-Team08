package com.team08.backend.domain.couponpolicy.controller;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.service.CouponPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "관리자 쿠폰 정책 API", description = "관리자용 쿠폰 생성")
public class CouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    // [관리자] 쿠폰 정책 생성 /api/admin/coupons
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "쿠폰 정책 생성", description = "새로운 쿠폰 정책(할인액, 유효기간, 발급방식 등)을 생성합니다.")
    public CouponPolicyResponse createCoupon(@Valid @RequestBody CouponPolicyCreateRequest request) {
        // TODO 관리자 인증 추가
        return couponPolicyService.createCouponPolicy(request);
    }
}

