package com.team08.backend.domain.payment.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        given(transactionService.findReadyIds()).willReturn(List.of(1L));
        willThrow(new IllegalStateException("후처리 실패"))
                .given(transactionService).processReady(1L);

        processor.processReady();

        verify(transactionService).markFailed(1L, "후처리 실패");
    }
}
