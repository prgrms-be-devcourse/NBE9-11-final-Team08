package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import com.team08.backend.domain.issuedcoupon.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NormalIssuedCouponStrategy implements IssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Override
    public CouponType getSupportedType() {
        return CouponType.NORMAL;
    }

    // [사용자] 일반 쿠폰 발급 로직
    @Override
    @Transactional
    public IssuedCoupon issue(Long userId, Long policyId) {
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);

        // 중복 발급 체크
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new CouponAlreadyIssuedException();
        }

        // 쿠폰 발급 기간 검증
        policy.validateIssuePeriod();

        // 쿠폰 발급 기록 생성
        IssuedCoupon newCoupon = IssuedCoupon.create(policy, userId);

        // 동시성 방어
        return issuedCouponRepository.saveWithConcurrencyProtection(newCoupon);
    }
}
