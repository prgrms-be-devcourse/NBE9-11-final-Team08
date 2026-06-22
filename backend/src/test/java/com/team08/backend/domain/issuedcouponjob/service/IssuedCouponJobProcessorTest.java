package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @DisplayName("성공: 쿠폰 발급 작업 처리 중 예외가 발생하면 재시도 상태로 변경한다")
    void process_fail_markRetrying() {
        // given
        Long jobId = 1L;
        when(issuedCouponJobWriter.markProcessing(jobId, java.time.LocalDateTime.now(clock))).thenReturn(true);
        doThrow(new IllegalStateException()).when(issuedCouponJobIssuer).issueCoupon(jobId);

        // when
        issuedCouponJobProcessor.process(jobId);

        // then
        verify(issuedCouponJobWriter).markRetrying(jobId, "IllegalStateException", java.time.LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("성공: 재시도 불가능한 쿠폰 정책 예외가 발생하면 실패 상태로 변경한다")
    void process_couponPolicyException_markDead() {
        // given
        Long jobId = 1L;
        when(issuedCouponJobWriter.markProcessing(jobId, java.time.LocalDateTime.now(clock))).thenReturn(true);
        doThrow(new CouponExhaustedException()).when(issuedCouponJobIssuer).issueCoupon(jobId);

        // when
        issuedCouponJobProcessor.process(jobId);

        // then
        verify(issuedCouponJobWriter).markDead(jobId, "CouponExhaustedException", java.time.LocalDateTime.now(clock));
        verify(issuedCouponJobWriter, never()).markRetrying(jobId, "CouponExhaustedException", java.time.LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("성공: 다른 처리자가 이미 선점한 작업이면 발급을 실행하지 않는다")
    void process_alreadyProcessing_skip() {
        // given
        Long jobId = 1L;
        when(issuedCouponJobWriter.markProcessing(jobId, java.time.LocalDateTime.now(clock))).thenReturn(false);

        // when
        issuedCouponJobProcessor.process(jobId);

        // then
        verify(issuedCouponJobIssuer, never()).issueCoupon(jobId);
    }
}
