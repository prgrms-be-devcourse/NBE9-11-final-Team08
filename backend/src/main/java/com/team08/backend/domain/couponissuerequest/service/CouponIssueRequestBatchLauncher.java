package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CouponIssueRequestBatchLauncher {

    private final JobLauncher jobLauncher;
    private final Job couponIssueAllUsersJob;
    private final CouponIssueRequestRepository couponIssueRequestRepository;
    private final Clock clock;

    public CouponIssueRequestBatchLauncher(
            JobLauncher jobLauncher,
            @Qualifier("couponIssueAllUsersJob") Job couponIssueAllUsersJob,
            CouponIssueRequestRepository couponIssueRequestRepository,
            Clock clock
    ) {
        this.jobLauncher = jobLauncher;
        this.couponIssueAllUsersJob = couponIssueAllUsersJob;
        this.couponIssueRequestRepository = couponIssueRequestRepository;
        this.clock = clock;
    }

    @Async
    public void launchAllUsersIssueJob(Long requestId, Long policyId, String issueKey, LocalDateTime requestedAt) {
        try {
            jobLauncher.run(couponIssueAllUsersJob, createJobParameters(requestId, policyId, issueKey, requestedAt));
        } catch (Exception e) {
            log.error("[전체 회원 쿠폰 발급 배치] 실행 실패. requestId={}, reason={}", requestId, e.getMessage(), e);
            couponIssueRequestRepository.markFailed(
                    requestId,
                    e.getMessage(),
                    LocalDateTime.now(clock)
            );
        }
    }

    private JobParameters createJobParameters(Long requestId, Long policyId, String issueKey, LocalDateTime requestedAt) {
        return new JobParametersBuilder()
                .addLong("requestId", requestId)
                .addLong("policyId", policyId)
                .addString("issueKey", issueKey)
                .addString("requestedAt", requestedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
    }
}
