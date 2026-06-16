package com.team08.backend.domain.couponpolicy.service;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyResponse;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.factory.CouponPolicyFactory;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponPolicyFactory couponPolicyFactory;

    @Transactional
    public CouponPolicyResponse createCouponPolicy(CouponPolicyCreateRequest request) {
        CouponPolicy newPolicy = couponPolicyFactory.create(request);
        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);

        return CouponPolicyResponse.from(savedPolicy);
    }
}
