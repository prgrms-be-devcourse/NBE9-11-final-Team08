package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.issued-coupon-job.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class IssuedCouponJobRetryScheduler {

    private static final long PROCESSING_TIMEOUT_MINUTES = 5;

    private final IssuedCouponJobRepository issuedCouponJobRepository;
    private final IssuedCouponJobProcessor issuedCouponJobProcessor;
    private final IssuedCouponJobWriter issuedCouponJobWriter;
    private final Clock clock;

    // 선착순 쿠폰 발급 실패 작업 재처리
    @Scheduled(fixedDelay = 5000)
    public void retryJobs() {
        issuedCouponJobWriter.recoverStuckProcessingJobs(
                LocalDateTime.now(clock).minusMinutes(PROCESSING_TIMEOUT_MINUTES)
        );

        List<IssuedCouponJob> jobs = issuedCouponJobRepository.findTop100ByStatusInOrderByRequestedAtAsc(
                List.of(IssuedCouponJobStatus.RETRYING)
        );

        for (IssuedCouponJob job : jobs) {
            issuedCouponJobProcessor.process(job.getId());
        }
    }
}
