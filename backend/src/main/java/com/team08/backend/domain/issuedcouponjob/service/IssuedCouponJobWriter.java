package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobWriter {

    private final IssuedCouponJobRepository issuedCouponJobRepository;

    public record JobLockResult(IssuedCouponJob job, boolean isAcquired) {
    }

    // 쿠폰 발급 작업 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobLockResult tryAcquireProcessing(String requestId, Long userId, Long policyId, LocalDateTime now) {
        IssuedCouponJob job = issuedCouponJobRepository.findByRequestId(requestId).orElse(null);

        if (job == null) {
            try {
                IssuedCouponJob newJob = IssuedCouponJob.request(requestId, userId, policyId, now);
                newJob.startProcessing(now);
                issuedCouponJobRepository.saveAndFlush(newJob);
                return new JobLockResult(newJob, true);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                job = issuedCouponJobRepository.findByRequestId(requestId).orElseThrow();
            }
        }

        if (job.getStatus() == IssuedCouponJobStatus.ISSUED || job.getStatus() == IssuedCouponJobStatus.FAILED) {
            return new JobLockResult(job, false);
        }

        LocalDateTime staleThreshold = now.minusMinutes(5);
        int updatedCount = issuedCouponJobRepository.acquireProcessingLock(
                requestId,
                IssuedCouponJobStatus.PROCESSING,
                List.of(IssuedCouponJobStatus.REQUESTED, IssuedCouponJobStatus.RETRY),
                now,
                staleThreshold
        );

        if (updatedCount > 0) {
            job.startProcessing(now);
            return new JobLockResult(job, true);
        } else {
            return new JobLockResult(job, false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessing(Long jobId, LocalDateTime startedAt) {
        int updatedCount = issuedCouponJobRepository.markProcessing(
                jobId,
                IssuedCouponJobStatus.PROCESSING,
                List.of(IssuedCouponJobStatus.REQUESTED),
                startedAt
        );
        return updatedCount == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markIssued(Long jobId, LocalDateTime completedAt) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId).orElseThrow();
        job.markIssued(completedAt);
        issuedCouponJobRepository.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long jobId, LocalDateTime now) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId).orElseThrow();
        job.markFailed(now);
        issuedCouponJobRepository.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetry(Long jobId, LocalDateTime now) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId).orElseThrow();
        job.markRetry(now);
        issuedCouponJobRepository.flush();
    }
}
