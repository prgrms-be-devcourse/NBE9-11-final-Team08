package com.team08.backend.domain.payment.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessOutboxServiceTest {

    @Mock
    private PaymentSuccessOutboxRepository paymentSuccessOutboxRepository;

    @Test
    void createsPendingEventOncePerPayment() {
        PaymentSuccessOutboxService service = new PaymentSuccessOutboxService(paymentSuccessOutboxRepository);
        given(paymentSuccessOutboxRepository.existsByPaymentId(100L)).willReturn(false);

        service.createIfAbsent(100L, 10L, 1L);

        ArgumentCaptor<PaymentSuccessOutboxEvent> captor =
                ArgumentCaptor.forClass(PaymentSuccessOutboxEvent.class);
        verify(paymentSuccessOutboxRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentId()).isEqualTo(100L);
        assertThat(captor.getValue().getOrderId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getEventType())
                .isEqualTo(PaymentSuccessOutboxEvent.PAYMENT_SUCCESS_POST_PROCESSING);
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentSuccessOutboxStatus.PENDING);
    }

    @Test
    void doesNotCreateDuplicateEventForSamePayment() {
        PaymentSuccessOutboxService service = new PaymentSuccessOutboxService(paymentSuccessOutboxRepository);
        given(paymentSuccessOutboxRepository.existsByPaymentId(100L)).willReturn(true);

        service.createIfAbsent(100L, 10L, 1L);

        verify(paymentSuccessOutboxRepository, never()).save(any());
    }
}
