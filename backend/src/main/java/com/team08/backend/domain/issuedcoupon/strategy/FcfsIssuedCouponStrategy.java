package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.exception.CouponIssueFailedException;
import com.team08.backend.domain.issuedcoupon.service.FcfsCouponRedisIssuer;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobStreamPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class FcfsIssuedCouponStrategy extends AbstractIssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;
    private final FcfsCouponRedisIssuer fcfsCouponRedisIssuer;
    private final IssuedCouponJobStreamPublisher issuedCouponJobStreamPublisher;

    public FcfsIssuedCouponStrategy(
            Clock clock,
            CouponPolicyRepository couponPolicyRepository,
            FcfsCouponRedisIssuer fcfsCouponRedisIssuer,
            IssuedCouponJobStreamPublisher issuedCouponJobStreamPublisher
    ) {
        super(clock);
        this.couponPolicyRepository = couponPolicyRepository;
        this.fcfsCouponRedisIssuer = fcfsCouponRedisIssuer;
        this.issuedCouponJobStreamPublisher = issuedCouponJobStreamPublisher;
    }

    @Override
    public CouponType getSupportedType() {
        return CouponType.FCFS;
    }

    // 선착순 쿠폰 정책 조회
    @Override
    protected CouponPolicy findPolicy(Long policyId) {
        return couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
    }

    // 선착순 쿠폰 중복 발급 체크
    @Override
    protected void validateDuplicateIssue(Long userId, Long policyId) {
    }

    @Override
    protected CouponIssueResult processIssue(Long userId, CouponPolicy policy, LocalDateTime now) {
        // Redis 선착순 발급 확정
        fcfsCouponRedisIssuer.issue(userId, policy);

        String requestId = UUID.randomUUID().toString();
        try {
            issuedCouponJobStreamPublisher.publish(requestId, userId, policy.getId());
        } catch (Exception e) {
            fcfsCouponRedisIssuer.rollback(userId, policy.getId());
            throw new CouponIssueFailedException();
        }
        return CouponIssueResult.requested(userId, policy.getId());
    }
}
