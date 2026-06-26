package com.team08.backend.domain.issuedcoupon.batch;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CouponExpirationBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Bean
    public Job couponExpirationJob() {
        return new JobBuilder("couponExpirationJob", jobRepository)
                .start(couponExpirationStep())
                .build();
    }

    @Bean
    public Step couponExpirationStep() {
        return new StepBuilder("couponExpirationStep", jobRepository)
                .tasklet(couponExpirationTasklet(null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet couponExpirationTasklet(
            @Value("#{jobParameters['now']}") String nowStr
    ) {
        LocalDateTime now = (nowStr != null && !nowStr.isEmpty())
                ? LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now(clock);

        AtomicInteger totalUpdated = new AtomicInteger();
        log.info("couponExpirationTasklet initialized with now = {}", now);

        return (contribution, chunkContext) -> {
            int updated = jdbcTemplate.update("""
                            UPDATE issued_coupons
                            SET status = ?
                            WHERE status = ?
                              AND expired_at < ?
                            ORDER BY expired_at, id
                            LIMIT ?
                            """,
                    CouponStatus.EXPIRED.name(),
                    CouponStatus.ISSUED.name(),
                    now,
                    CHUNK_SIZE
            );

            contribution.incrementWriteCount(updated);
            if (updated == 0) {
                log.info("[Batch] 쿠폰 만료 처리 완료. totalUpdated={}", totalUpdated.get());
                return RepeatStatus.FINISHED;
            }

            int total = totalUpdated.addAndGet(updated);
            log.debug("[Batch] 쿠폰 {}건 EXPIRED 처리 완료. totalUpdated={}", updated, total);
            return RepeatStatus.CONTINUABLE;
        };
    }
}
