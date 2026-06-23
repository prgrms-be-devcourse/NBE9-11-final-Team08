package com.team08.backend.domain.issuedcouponjob.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("성공: 쿠폰 발급 작업 처리 중 예외가 발생하면 그대로 전파한다")
    void process_fail_throwException() {
        // given
        Long jobId = 1L;
        when(issuedCouponJobWriter.markProcessing(jobId, java.time.LocalDateTime.now(clock))).thenReturn(true);
        doThrow(new IllegalStateException()).when(issuedCouponJobIssuer).issueCoupon(jobId);

        // when & then
        assertThatThrownBy(() -> issuedCouponJobProcessor.process(jobId))
                .isInstanceOf(IllegalStateException.class);
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
