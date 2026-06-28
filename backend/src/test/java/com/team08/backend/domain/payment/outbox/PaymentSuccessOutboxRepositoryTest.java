package com.team08.backend.domain.payment.outbox;

import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class PaymentSuccessOutboxRepositoryTest {

    @Autowired
    private PaymentSuccessOutboxRepository paymentSuccessOutboxRepository;

    @Test
    void findsPendingAndOnlyDueFailedEventsInOldestOrder() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 19, 0);
        PaymentSuccessOutboxEvent pending = saveEvent(100L, 10L);

        PaymentSuccessOutboxEvent futureFailed = event(101L, 11L);
        futureFailed.markFailed("후처리 실패", now, 5, 10);
        save(futureFailed);

        PaymentSuccessOutboxEvent dueFailed = event(102L, 12L);
        dueFailed.markFailed("후처리 실패", now.minusSeconds(20), 5, 10);
        save(dueFailed);

        List<PaymentSuccessOutboxEvent> result = paymentSuccessOutboxRepository.findReady(
                PaymentSuccessOutboxStatus.PENDING,
                PaymentSuccessOutboxStatus.FAILED,
                now,
                PageRequest.of(0, 10)
        );

        assertThat(result)
                .extracting(PaymentSuccessOutboxEvent::getId)
                .containsExactly(pending.getId(), dueFailed.getId());
    }

    private PaymentSuccessOutboxEvent saveEvent(Long paymentId, Long orderId) {
        return save(event(paymentId, orderId));
    }

    private PaymentSuccessOutboxEvent save(PaymentSuccessOutboxEvent event) {
        return paymentSuccessOutboxRepository.saveAndFlush(event);
    }

    private PaymentSuccessOutboxEvent event(Long paymentId, Long orderId) {
        return PaymentSuccessOutboxEvent.paymentSucceeded(paymentId, orderId, 1L);
    }
}
