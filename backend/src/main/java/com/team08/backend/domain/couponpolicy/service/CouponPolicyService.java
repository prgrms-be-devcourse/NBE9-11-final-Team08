package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicyResponse createCouponPolicy(CouponPolicyCreateRequest request) {
        CouponPolicy newPolicy = CouponPolicy.create(
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.minOrderAmount(),
                request.validDays(),
                request.totalQuantity(),
                request.categoryId(),
                request.courseIds(),
                request.couponType(),
                request.couponTarget(),
                request.usageType(),
                request.isStackable(),
                request.issueStartDate(),
                request.issueEndDate()
        );
        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);

        return CouponPolicyResponse.from(savedPolicy);
    }
}
