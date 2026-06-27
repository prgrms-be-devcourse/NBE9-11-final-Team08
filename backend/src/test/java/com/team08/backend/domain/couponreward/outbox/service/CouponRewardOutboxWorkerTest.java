package com.team08.backend.domain.couponreward.outbox.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class CouponRewardOutboxWorkerTest {

    @Mock
    private CouponRewardOutboxTransactionService couponRewardOutboxTransactionService;

    @Test
    @DisplayName("outbox 이벤트 처리 성공 시 성공 트랜잭션만 호출한다")
    void processOne_callsIssueAndMarkProcessed() {
        // given
        CouponRewardOutboxWorker worker = new CouponRewardOutboxWorker(couponRewardOutboxTransactionService);

        // when
        worker.processOne(10L);

        // then
        then(couponRewardOutboxTransactionService).should().issueAndMarkProcessed(10L);
        then(couponRewardOutboxTransactionService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("outbox 이벤트 처리 실패 시 별도 실패 기록 트랜잭션을 호출한다")
    void processOne_callsMarkFailedWhenIssueFails() {
        // given
        CouponRewardOutboxWorker worker = new CouponRewardOutboxWorker(couponRewardOutboxTransactionService);
        IllegalStateException exception = new IllegalStateException("failed");
        willThrow(exception).given(couponRewardOutboxTransactionService).issueAndMarkProcessed(10L);

        // when
        worker.processOne(10L);

        // then
        then(couponRewardOutboxTransactionService).should().markFailed(10L, exception);
    }
}
