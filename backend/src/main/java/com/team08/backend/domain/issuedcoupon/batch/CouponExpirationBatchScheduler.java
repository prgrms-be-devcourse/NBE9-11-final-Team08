package com.team08.backend.domain.issuedcoupon.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CouponExpirationBatchScheduler {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);
    private static final String LOCK_NAME = "couponExpirationJob";

    private final JobLauncher jobLauncher;
    private final Job couponExpirationJob;
    private final TaskScheduler retryTaskScheduler;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public CouponExpirationBatchScheduler(
            JobLauncher jobLauncher,
            Job couponExpirationJob,
            @Qualifier("couponExpirationRetryTaskScheduler") TaskScheduler retryTaskScheduler,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.jobLauncher = jobLauncher;
        this.couponExpirationJob = couponExpirationJob;
        this.retryTaskScheduler = retryTaskScheduler;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runCouponExpirationJob() {
        runCouponExpirationJobWithLock(0, LocalDateTime.now(clock), "scheduled");
    }

    private void runCouponExpirationJobWithLock(int retryAttempt, LocalDateTime now, String trigger) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            if (!tryAcquireLock(connection)) {
                log.info("[쿠폰 만료 배치] 다른 인스턴스에서 실행 중이므로 건너뜁니다. trigger={}, retryAttempt={}",
                        trigger, retryAttempt);
                return null;
            }

            try {
                runCouponExpirationJob(retryAttempt, now);
            } finally {
                releaseLock(connection);
            }

            return null;
        });
    }

    private void runCouponExpirationJob(int retryAttempt, LocalDateTime now) {
        try {
            log.info("[쿠폰 만료 배치] 실행 시작. 기준시각={}, retryAttempt={}", now, retryAttempt);
            JobExecution jobExecution = jobLauncher.run(couponExpirationJob, createJobParameters(now, retryAttempt));

            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                log.info("[쿠폰 만료 배치] 실행 완료. 완료시각={}, retryAttempt={}",
                        LocalDateTime.now(clock), retryAttempt);
                return;
            }

            scheduleRetry(now, retryAttempt, "status=" + jobExecution.getStatus()
                    + ", exitStatus=" + jobExecution.getExitStatus());
        } catch (Exception e) {
            scheduleRetry(now, retryAttempt, e.getMessage());
            log.error("[쿠폰 만료 배치] 실행 실패. reason={}", e.getMessage(), e);
        }
    }

    private JobParameters createJobParameters(LocalDateTime now, int retryAttempt) {
        return new JobParametersBuilder()
                .addString("now", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("retryAttempt", (long) retryAttempt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
    }

    private void scheduleRetry(LocalDateTime now, int retryAttempt, String failureReason) {
        int nextRetryAttempt = retryAttempt + 1;

        if (nextRetryAttempt > MAX_RETRY_ATTEMPTS) {
            log.error("[쿠폰 만료 배치] 최대 재시도 횟수를 초과했습니다. retryAttempt={}, reason={}",
                    retryAttempt, failureReason);
            return;
        }

        Instant retryAt = clock.instant().plus(RETRY_DELAY);
        log.warn("[쿠폰 만료 배치] 재시도를 예약합니다. retryAttempt={}/{}, retryAt={}, reason={}",
                nextRetryAttempt, MAX_RETRY_ATTEMPTS, retryAt, failureReason);

        retryTaskScheduler.schedule(
                () -> runCouponExpirationJobWithLock(nextRetryAttempt, now, "retry"),
                retryAt
        );
    }

    private boolean tryAcquireLock(java.sql.Connection connection) throws java.sql.SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT GET_LOCK(?, 0)")) {
            ps.setString(1, LOCK_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private void releaseLock(java.sql.Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, LOCK_NAME);
            ps.execute();
        } catch (Exception e) {
            log.error("[쿠폰 만료 배치] 분산락 해제 실패. reason={}", e.getMessage(), e);
        }
    }
}
