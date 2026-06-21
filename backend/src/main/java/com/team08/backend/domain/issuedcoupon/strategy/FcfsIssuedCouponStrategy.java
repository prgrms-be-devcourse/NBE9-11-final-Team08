package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class FcfsIssuedCouponStrategy extends AbstractIssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;

    public FcfsIssuedCouponStrategy(
            IssuedCouponRepository issuedCouponRepository,
            Clock clock,
            CouponPolicyRepository couponPolicyRepository
    ) {
        super(issuedCouponRepository, clock);
        this.couponPolicyRepository = couponPolicyRepository;
    }

    @Override
    public CouponType getSupportedType() {
        return CouponType.FCFS;
    }

    // 선착순 쿠폰 정책 조회
    @Override
    protected CouponPolicy findPolicy(Long policyId) {
        // 비관적 락을 적용한 쿠폰 정책 조회
        return couponPolicyRepository.findByIdWithLock(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
    }

    // 선착순 쿠폰 발급 전 처리
    @Override
    protected void beforeIssue(CouponPolicy policy) {
        // 쿠폰 수량 차감 및 재고 소진 체크
        policy.decreaseQuantity();
    }
}
