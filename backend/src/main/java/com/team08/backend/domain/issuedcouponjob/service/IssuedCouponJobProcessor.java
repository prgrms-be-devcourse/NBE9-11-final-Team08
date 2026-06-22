package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.couponpolicy.exception.CouponPolicyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobProcessor {

    private final IssuedCouponJobWriter issuedCouponJobWriter;
    private final IssuedCouponJobIssuer issuedCouponJobIssuer;
    private final Clock clock;

    // 쿠폰 발급 작업 처리
    public void process(Long jobId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!issuedCouponJobWriter.markProcessing(jobId, now)) {
            return;
        }

        try {
            issuedCouponJobIssuer.issueCoupon(jobId);
        } catch (CouponPolicyException e) {
            issuedCouponJobWriter.markDead(jobId, e.getClass().getSimpleName(), LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            issuedCouponJobWriter.markRetrying(jobId, e.getClass().getSimpleName(), LocalDateTime.now(clock));
        }
    }
}
