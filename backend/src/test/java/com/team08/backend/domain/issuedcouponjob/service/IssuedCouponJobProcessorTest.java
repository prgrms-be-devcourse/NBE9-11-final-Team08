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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
        doThrow(new IllegalStateException()).when(issuedCouponJobIssuer).issueCoupon(jobId);

        // when
        issuedCouponJobProcessor.process(jobId);

        // then
        verify(issuedCouponJobWriter).markRetrying(jobId, "IllegalStateException", java.time.LocalDateTime.now(clock));
    }
}
