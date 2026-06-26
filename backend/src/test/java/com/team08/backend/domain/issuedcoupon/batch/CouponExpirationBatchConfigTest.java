package com.team08.backend.domain.issuedcoupon.batch;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(properties = "spring.batch.jdbc.initialize-schema=always")
@ActiveProfiles("test")
class CouponExpirationBatchConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    private CouponPolicy mockPolicy;

    @BeforeEach
    void setUp() {
        mockPolicy = CouponPolicy.createPolicy(
                "Test Policy",
                CouponTarget.ALL,
                CouponType.NORMAL,
                100,
                CouponUsageType.SINGLE_USE,
                false,
                DiscountType.AMOUNT,
                1000,
                null,
                null,
                30,
                null,
                null,
                null,
                null
        );
        couponPolicyRepository.save(mockPolicy);
    }

    @AfterEach
    void tearDown() {
        issuedCouponRepository.deleteAllInBatch();
        couponPolicyRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("만료일이 지난 쿠폰은 EXPIRED 상태로 변경된다.")
    void expireCouponsTest() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);
        LocalDateTime future = now.plusDays(1);

        // 만료된 쿠폰 2개
        IssuedCoupon expired1 = createCoupon(1L, past, now.minusDays(30));
        IssuedCoupon expired2 = createCoupon(2L, past, now.minusDays(30));

        // 유효한 쿠폰 1개
        IssuedCoupon valid = createCoupon(3L, future, now.minusDays(5));

        issuedCouponRepository.save(expired1);
        issuedCouponRepository.save(expired2);
        issuedCouponRepository.save(valid);

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("now", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("time", System.currentTimeMillis()) // To ensure job uniqueness
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        IssuedCoupon findExpired1 = issuedCouponRepository.findById(expired1.getId()).orElseThrow();
        IssuedCoupon findExpired2 = issuedCouponRepository.findById(expired2.getId()).orElseThrow();
        IssuedCoupon findValid = issuedCouponRepository.findById(valid.getId()).orElseThrow();

        assertThat(findExpired1.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        assertThat(findExpired2.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        assertThat(findValid.getStatus()).isEqualTo(CouponStatus.ISSUED);
    }

    @Test
    @DisplayName("만료 대상이 청크 크기를 초과해도 반복 실행으로 모두 EXPIRED 상태로 변경된다.")
    void expireCouponsOverChunkSizeTest() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);
        List<IssuedCoupon> expiredCoupons = new ArrayList<>();

        for (long userId = 1; userId <= 1001; userId++) {
            expiredCoupons.add(createCoupon(userId, past, now.minusDays(30)));
        }
        issuedCouponRepository.saveAll(expiredCoupons);

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("now", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(issuedCouponRepository.findAll())
                .hasSize(1001)
                .allMatch(coupon -> coupon.getStatus() == CouponStatus.EXPIRED);
    }

    private IssuedCoupon createCoupon(Long userId, LocalDateTime expiredAt, LocalDateTime issuedAt) {
        IssuedCoupon coupon = IssuedCoupon.create(mockPolicy, userId, issuedAt);
        ReflectionTestUtils.setField(coupon, "expiredAt", expiredAt);
        return coupon;
    }
}
