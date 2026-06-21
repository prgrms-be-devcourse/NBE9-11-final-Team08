package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobWriter {

    private final IssuedCouponJobRepository issuedCouponJobRepository;

    // 쿠폰 발급 작업 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IssuedCouponJob createRequested(Long userId, Long policyId, LocalDateTime requestedAt) {
        return issuedCouponJobRepository.save(
                IssuedCouponJob.request(userId, policyId, requestedAt)
        );
    }

    // 쿠폰 발급 성공 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markIssued(Long jobId, LocalDateTime completedAt) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        job.markIssued(completedAt);
    }

    // 쿠폰 발급 실패 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long jobId, String failureReason, LocalDateTime completedAt) {
        IssuedCouponJob job = issuedCouponJobRepository.findById(jobId)
                .orElseThrow();
        job.markFailed(failureReason, completedAt);
    }
}
