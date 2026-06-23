package com.team08.backend.domain.issuedcouponjob.service;

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

        issuedCouponJobIssuer.issueCoupon(jobId);
    }
}
