package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobIssueExecutor {

    private final IssuedCouponJobRepository issuedCouponJobRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    // 쿠폰 발급 작업 DB 반영
    @Transactional
    public void issue(Long jobId) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        if (!job.isProcessable()) {
            return;
        }

        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(job.getPolicyId())
                .orElseThrow(CouponPolicyNotFoundException::new);

        if (issuedCouponRepository.existsByUserIdAndPolicyId(job.getUserId(), job.getPolicyId())) {
            job.markIssued(LocalDateTime.now(clock));
            return;
        }

        policy.decreaseQuantity();
        IssuedCoupon issuedCoupon = IssuedCoupon.create(policy, job.getUserId(), LocalDateTime.now(clock));
        issuedCouponRepository.saveAndFlush(issuedCoupon);
        job.markIssued(LocalDateTime.now(clock));
    }
}
