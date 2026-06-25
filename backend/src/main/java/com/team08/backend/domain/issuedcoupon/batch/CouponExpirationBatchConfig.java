package com.team08.backend.domain.issuedcoupon.batch;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CouponExpirationBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final Clock clock;

    @Bean
    public Job couponExpirationJob() {
        return new JobBuilder("couponExpirationJob", jobRepository)
                .start(couponExpirationStep())
                .build();
    }

    @Bean
    @JobScope
    public Step couponExpirationStep() {
        return new StepBuilder("couponExpirationStep", jobRepository)
                .<IssuedCoupon, IssuedCoupon>chunk(CHUNK_SIZE, transactionManager)
                .reader(couponExpirationReader(null))
                .processor(couponExpirationProcessor())
                .writer(couponExpirationWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<IssuedCoupon> couponExpirationReader(
            @Value("#{jobParameters['now']}") String nowStr
    ) {
        LocalDateTime now = (nowStr != null && !nowStr.isEmpty())
                ? LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now(clock);

        log.info("couponExpirationReader initialized with now = {}", now);

        return new JpaPagingItemReaderBuilder<IssuedCoupon>()
                .name("couponExpirationReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT c FROM IssuedCoupon c WHERE c.status = :status AND c.expiredAt < :now")
                .parameterValues(Map.of(
                        "status", CouponStatus.ISSUED,
                        "now", now
                ))
                .build();
    }

    @Bean
    public ItemProcessor<IssuedCoupon, IssuedCoupon> couponExpirationProcessor() {
        return coupon -> {
            coupon.expire();
            log.debug("Coupon {} expired.", coupon.getId());
            return coupon;
        };
    }

    @Bean
    public JpaItemWriter<IssuedCoupon> couponExpirationWriter() {
        return new JpaItemWriterBuilder<IssuedCoupon>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
