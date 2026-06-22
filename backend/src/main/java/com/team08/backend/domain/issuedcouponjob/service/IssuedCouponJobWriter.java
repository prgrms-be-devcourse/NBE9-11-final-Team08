package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IssuedCouponJobWriter {

    private static final int MAX_RETRY_COUNT = 5;

    private final IssuedCouponJobRepository issuedCouponJobRepository;

    // 쿠폰 발급 작업 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IssuedCouponJob createRequested(Long userId, Long policyId, LocalDateTime requestedAt) {
        return issuedCouponJobRepository.save(
                IssuedCouponJob.request(userId, policyId, requestedAt)
        );
    }

    // 처리 가능한 작업을 PROCESSING 상태로 선점
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessing(Long jobId, LocalDateTime startedAt) {
        int updatedCount = issuedCouponJobRepository.markProcessing(
                jobId,
                IssuedCouponJobStatus.PROCESSING,
                List.of(IssuedCouponJobStatus.REQUESTED, IssuedCouponJobStatus.RETRYING),
                startedAt
        );
        return updatedCount == 1;
    }

    // 쿠폰 발급 성공 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markIssued(Long jobId, LocalDateTime completedAt) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        job.markIssued(completedAt);
        issuedCouponJobRepository.flush();
    }

    // 쿠폰 발급 재시도 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetrying(Long jobId, String failureReason, LocalDateTime failedAt) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        job.markRetrying(failureReason, failedAt, MAX_RETRY_COUNT);
        issuedCouponJobRepository.flush();

        if (job.getStatus() == IssuedCouponJobStatus.DEAD) {
            log.error("선착순 쿠폰 발급 작업 자동 복구 실패. jobId={}, userId={}, policyId={}, retryCount={}, failureReason={}",
                    job.getId(), job.getUserId(), job.getPolicyId(), job.getRetryCount(), job.getFailureReason());
            return;
        }

        log.warn("선착순 쿠폰 발급 작업 재시도 대기. jobId={}, userId={}, policyId={}, retryCount={}, failureReason={}",
                job.getId(), job.getUserId(), job.getPolicyId(), job.getRetryCount(), job.getFailureReason());
    }

    // 쿠폰 소진 등 재시도해도 복구할 수 없는 실패 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDead(Long jobId, String failureReason, LocalDateTime failedAt) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        job.markDead(failureReason, failedAt);
        issuedCouponJobRepository.flush();

        log.warn("선착순 쿠폰 발급 작업 복구 불가. jobId={}, userId={}, policyId={}, retryCount={}, failureReason={}",
                job.getId(), job.getUserId(), job.getPolicyId(), job.getRetryCount(), job.getFailureReason());
    }
}
