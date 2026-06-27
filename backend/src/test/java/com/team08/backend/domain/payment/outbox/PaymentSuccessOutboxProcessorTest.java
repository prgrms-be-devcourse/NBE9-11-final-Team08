package com.team08.backend.domain.payment.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessOutboxProcessorTest {

    @Mock
    private PaymentSuccessOutboxTransactionService transactionService;

    @Test
    void processingFailureMarksEventFailed() {
        PaymentSuccessOutboxProcessor processor = new PaymentSuccessOutboxProcessor(transactionService);
        ReflectionTestUtils.setField(processor, "batchSize", 20);
        given(transactionService.findPendingIds(20)).willReturn(List.of(1L));
        willThrow(new IllegalStateException("후처리 실패"))
                .given(transactionService).processPending(1L);

        processor.processPending();

        verify(transactionService).markFailed(1L, "후처리 실패");
    }
}
