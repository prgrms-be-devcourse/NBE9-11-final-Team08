package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public void createCouponPolicy(CouponPolicyCreateRequest request) {
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
    }
}
