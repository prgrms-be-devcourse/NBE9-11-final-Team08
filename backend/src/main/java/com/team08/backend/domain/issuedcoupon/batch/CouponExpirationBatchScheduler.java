package com.team08.backend.domain.issuedcoupon.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpirationBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job couponExpirationJob;
    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * *")
    public void runCouponExpirationJob() {
        LocalDateTime now = LocalDateTime.now(clock);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("now", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            log.info("Starting couponExpirationJob at {}", now);
            jobLauncher.run(couponExpirationJob, jobParameters);
            log.info("Successfully finished couponExpirationJob at {}", LocalDateTime.now(clock));

        } catch (Exception e) {
            log.error("Failed to run couponExpirationJob: {}", e.getMessage(), e);
        }
    }
}
