package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcoupon.exception.JobAlreadyProcessingException;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobWriter.JobLockResult;

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

    public void process(String requestId, Long userId, Long policyId) {
        LocalDateTime now = LocalDateTime.now(clock);
        
        JobLockResult lockResult = issuedCouponJobWriter.tryAcquireProcessing(requestId, userId, policyId, now);
        IssuedCouponJob job = lockResult.job();

        if (!lockResult.isAcquired()) {
            if (job.getStatus() == IssuedCouponJobStatus.ISSUED || 
                job.getStatus() == IssuedCouponJobStatus.FAILED) {
                return;
            }
            throw new JobAlreadyProcessingException();
        }

        try {
            issuedCouponJobIssuer.issueCoupon(job.getId(), userId, policyId);
        } catch (CouponExhaustedException e) {
            issuedCouponJobWriter.markFailed(job.getId(), LocalDateTime.now(clock));
        } catch (Exception e) {
            try {
                issuedCouponJobWriter.markRetry(job.getId(), LocalDateTime.now(clock));
            } catch (Exception ignored) {}
            throw e;
        }
    }
}
