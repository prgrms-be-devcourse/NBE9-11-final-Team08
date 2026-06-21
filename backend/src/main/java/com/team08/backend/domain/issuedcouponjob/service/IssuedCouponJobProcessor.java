package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponWriter;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobProcessor {

    private final IssuedCouponJobRepository issuedCouponJobRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponWriter issuedCouponWriter;
    private final IssuedCouponJobWriter issuedCouponJobWriter;
    private final Clock clock;

    // 쿠폰 발급 작업 처리
    public void process(Long jobId) {
        try {
            IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                    .orElseThrow();
            if (!job.isProcessable()) {
                return;
            }

            CouponPolicy policy = couponPolicyRepository.findById(job.getPolicyId())
                    .orElseThrow(CouponPolicyNotFoundException::new);
            IssuedCoupon issuedCoupon = IssuedCoupon.create(policy, job.getUserId(), LocalDateTime.now(clock));
            try {
                issuedCouponWriter.saveWithConcurrencyProtection(issuedCoupon);
            } catch (CouponAlreadyIssuedException e) {
                issuedCouponJobWriter.markIssued(jobId, LocalDateTime.now(clock));
                return;
            }
            issuedCouponJobWriter.markIssued(jobId, LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            issuedCouponJobWriter.markRetrying(jobId, e.getClass().getSimpleName(), LocalDateTime.now(clock));
        }
    }
}
