package com.team08.backend.domain.couponpolicy.controller;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyDetailResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicySearchRequest;
import com.team08.backend.domain.couponpolicy.service.CouponPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "관리자 쿠폰 정책 API", description = "관리자용 쿠폰 생성 및 조회")
public class CouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    // [관리자] 쿠폰 정책 생성 /api/admin/coupons
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "쿠폰 정책 생성", description = "새로운 쿠폰 정책(할인액, 유효기간, 발급방식 등)을 생성합니다.")
    public CouponPolicyResponse createCoupon(@Valid @RequestBody CouponPolicyCreateRequest request) {
        return couponPolicyService.createCouponPolicy(request);
    }

    // [관리자] 쿠폰 정책 목록 조회 (검색 및 페이징) /api/admin/coupons
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "쿠폰 정책 목록 조회", description = "쿠폰명, 타입, 상태별 필터링과 페이징을 지원합니다.")
    public Page<CouponPolicyResponse> getCoupons(
            @ModelAttribute CouponPolicySearchRequest condition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return couponPolicyService.getCouponPolicies(condition, pageable);
    }

    // [관리자] 쿠폰 정책 상세 조회 /api/admin/coupons/{id}
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "쿠폰 정책 상세 조회", description = "특정 쿠폰 정책의 상세 정보와 적용 대상을 조회합니다.")
    public CouponPolicyDetailResponse getCoupon(@PathVariable Long id) {
        return couponPolicyService.getCouponPolicy(id);
    }
}

