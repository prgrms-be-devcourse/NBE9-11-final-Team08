package com.team08.backend.domain.issuedcouponjob.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobProcessor {

    private final IssuedCouponJobWriter issuedCouponJobWriter;
    private final IssuedCouponJobIssueExecutor issuedCouponJobIssueExecutor;
    private final Clock clock;

    // 쿠폰 발급 작업 처리
    public void process(Long jobId) {
        try {
            issuedCouponJobIssueExecutor.issue(jobId);
        } catch (RuntimeException e) {
            issuedCouponJobWriter.markRetrying(jobId, e.getClass().getSimpleName(), LocalDateTime.now(clock));
        }
    }
}
