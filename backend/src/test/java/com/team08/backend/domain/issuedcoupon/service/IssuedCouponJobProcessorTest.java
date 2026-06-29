package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcoupon.exception.JobAlreadyProcessingException;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponJobWriter.JobLockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuedCouponJobProcessorTest {

    @Mock
    private IssuedCouponJobWriter issuedCouponJobWriter;

    @Mock
    private IssuedCouponJobIssuer issuedCouponJobIssuer;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private IssuedCouponJobProcessor issuedCouponJobProcessor;

    @BeforeEach
    void setUp() {
        issuedCouponJobProcessor = new IssuedCouponJobProcessor(
                issuedCouponJobWriter,
                issuedCouponJobIssuer,
                clock
        );
    }

    @Test
    @DisplayName("성공: 쿠폰 발급 작업 처리 중 예외가 발생하면 그대로 전파한다")
    void process_fail_throwException() {
        // given
        Long userId = 1L;
        Long policyId = 2L;
        Long jobId = 10L;
        String requestId = UUID.randomUUID().toString();

        IssuedCouponJob job = Mockito.mock(IssuedCouponJob.class);
        when(job.getId()).thenReturn(jobId);

        JobLockResult lockResult = new JobLockResult(job, true);
        when(issuedCouponJobWriter.tryAcquireProcessing(requestId, userId, policyId, LocalDateTime.now(clock))).thenReturn(lockResult);
        doThrow(new IllegalStateException()).when(issuedCouponJobIssuer).issueCoupon(jobId, userId, policyId);

        // when & then
        assertThatThrownBy(() -> issuedCouponJobProcessor.process(requestId, userId, policyId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("성공: 락 획득 실패 시 JobAlreadyProcessingException을 던진다")
    void process_failToAcquireLock_throwException() {
        // given
        Long userId = 1L;
        Long policyId = 2L;
        Long jobId = 10L;
        String requestId = UUID.randomUUID().toString();

        IssuedCouponJob job = Mockito.mock(IssuedCouponJob.class);
        when(job.getStatus()).thenReturn(IssuedCouponJobStatus.PROCESSING);

        JobLockResult lockResult = new JobLockResult(job, false);
        when(issuedCouponJobWriter.tryAcquireProcessing(requestId, userId, policyId, LocalDateTime.now(clock))).thenReturn(lockResult);

        // when & then
        assertThatThrownBy(() -> issuedCouponJobProcessor.process(requestId, userId, policyId))
                .isInstanceOf(JobAlreadyProcessingException.class);
    }
}
