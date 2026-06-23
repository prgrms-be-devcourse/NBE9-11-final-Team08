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
                List.of(IssuedCouponJobStatus.REQUESTED),
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
}
