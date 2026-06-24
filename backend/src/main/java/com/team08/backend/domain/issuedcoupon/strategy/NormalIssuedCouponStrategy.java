package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class NormalIssuedCouponStrategy extends AbstractIssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    public NormalIssuedCouponStrategy(
            IssuedCouponRepository issuedCouponRepository,
            Clock clock,
            CouponPolicyRepository couponPolicyRepository
    ) {
        super(clock);
        this.issuedCouponRepository = issuedCouponRepository;
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

    // 일반 쿠폰 중복 발급 체크
    @Override
    protected void validateDuplicateIssue(Long userId, Long policyId) {
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new CouponAlreadyIssuedException();
        }
    }
}
