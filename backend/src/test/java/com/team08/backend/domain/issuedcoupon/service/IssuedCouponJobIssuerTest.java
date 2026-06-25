package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponJobRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuedCouponJobIssuerTest {

    @Mock
    private IssuedCouponJobRepository issuedCouponJobRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private IssuedCouponJobIssuer issuedCouponJobIssuer;

    @BeforeEach
    void setUp() {
        issuedCouponJobIssuer = new IssuedCouponJobIssuer(
                issuedCouponJobRepository,
                couponPolicyRepository,
                issuedCouponRepository,
                clock
        );
    }

    @Test
    @DisplayName("성공: 쿠폰 발급 작업을 DB에 반영하면 수량 차감 후 발급 쿠폰을 저장한다")
    void issueCoupon_success() {
        // given
        Long jobId = 1L;
        Long userId = 10L;
        Long policyId = 100L;
        IssuedCouponJob job = requestedJob(jobId, userId, policyId);
        CouponPolicy policy = fcfsPolicy(policyId);

        when(issuedCouponJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);

        // when
        issuedCouponJobIssuer.issueCoupon(jobId);

        // then
        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        verify(issuedCouponRepository).saveAndFlush(couponCaptor.capture());
        assertThat(policy.getTotalQuantity()).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(IssuedCouponJobStatus.ISSUED);
        assertThat(couponCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(couponCaptor.getValue().getPolicyId()).isEqualTo(policyId);
    }

    @Test
    @DisplayName("성공: 이미 발급된 작업은 수량을 다시 차감하지 않고 성공 처리한다")
    void issueCoupon_alreadyIssued_success() {
        // given
        Long jobId = 1L;
        Long userId = 10L;
        Long policyId = 100L;
        IssuedCouponJob job = requestedJob(jobId, userId, policyId);
        CouponPolicy policy = fcfsPolicy(policyId);

        when(issuedCouponJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(true);

        // when
        issuedCouponJobIssuer.issueCoupon(jobId);

        // then
        verify(issuedCouponRepository, never()).saveAndFlush(any());
        assertThat(policy.getTotalQuantity()).isEqualTo(2);
        assertThat(job.getStatus()).isEqualTo(IssuedCouponJobStatus.ISSUED);
    }

    private IssuedCouponJob requestedJob(Long jobId, Long userId, Long policyId) {
        IssuedCouponJob job = IssuedCouponJob.request(userId, policyId, LocalDateTime.now(clock));
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "status", IssuedCouponJobStatus.PROCESSING);
        return job;
    }

    private CouponPolicy fcfsPolicy(Long policyId) {
        CouponPolicy policy = CouponPolicy.createPolicy(
                "선착순 쿠폰",
                CouponTarget.ALL,
                CouponType.FCFS,
                2,
                CouponUsageType.SINGLE_USE,
                false,
                DiscountType.AMOUNT,
                1000,
                null,
                null,
                7,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(policy, "id", policyId);
        return policy;
    }
}
