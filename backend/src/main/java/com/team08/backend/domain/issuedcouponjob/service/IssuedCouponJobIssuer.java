package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobIssuer {

    private final IssuedCouponJobRepository issuedCouponJobRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    @Transactional
    public void issueCoupon(Long jobId, Long userId, Long policyId) {
        LocalDateTime now = LocalDateTime.now(clock);
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        if (!job.isProcessing()) {
            return;
        }

        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);

        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            job.markIssued(now);
            return;
        }

        if (policy.getTotalQuantity() != null) {
            int updated = couponPolicyRepository.decreaseQuantity(policyId);
            if (updated == 0) {
                throw new CouponExhaustedException();
            }
        }

        IssuedCoupon issuedCoupon = IssuedCoupon.create(policy, userId, now);
        issuedCouponRepository.saveAndFlush(issuedCoupon);
        job.markIssued(now);
    }
}
