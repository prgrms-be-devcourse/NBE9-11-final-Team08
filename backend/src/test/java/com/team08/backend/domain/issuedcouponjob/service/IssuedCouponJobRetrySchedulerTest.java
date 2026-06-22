package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuedCouponJobRetrySchedulerTest {

    @Mock
    private IssuedCouponJobRepository issuedCouponJobRepository;

    @Mock
    private IssuedCouponJobProcessor issuedCouponJobProcessor;

    @Mock
    private IssuedCouponJobWriter issuedCouponJobWriter;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private IssuedCouponJobRetryScheduler issuedCouponJobRetryScheduler;

    @BeforeEach
    void setUp() {
        issuedCouponJobRetryScheduler = new IssuedCouponJobRetryScheduler(
                issuedCouponJobRepository,
                issuedCouponJobProcessor,
                issuedCouponJobWriter,
                clock
        );
    }

    @Test
    @DisplayName("성공: 재시도 대상 작업을 조회해 다시 처리한다")
    void retryJobs_success() {
        // given
        IssuedCouponJob job = IssuedCouponJob.request(1L, 10L, LocalDateTime.now());
        ReflectionTestUtils.setField(job, "id", 100L);
        when(issuedCouponJobRepository.findTop100ByStatusInOrderByRequestedAtAsc(List.of(IssuedCouponJobStatus.RETRYING)))
                .thenReturn(List.of(job));

        // when
        issuedCouponJobRetryScheduler.retryJobs();

        // then
        verify(issuedCouponJobWriter).recoverStuckProcessingJobs(LocalDateTime.now(clock).minusMinutes(5));
        verify(issuedCouponJobProcessor).process(100L);
    }
}
