package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class NormalIssuedCouponStrategy extends AbstractIssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;

    public NormalIssuedCouponStrategy(
            IssuedCouponRepository issuedCouponRepository,
            Clock clock,
            CouponPolicyRepository couponPolicyRepository
    ) {
        super(issuedCouponRepository, clock);
        this.couponPolicyRepository = couponPolicyRepository;
    }

    @Override
    public CouponType getSupportedType() {
        return CouponType.NORMAL;
    }

    // 일반 쿠폰 정책 조회
    @Override
    protected CouponPolicy findPolicy(Long policyId) {
        return couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
    }
}
