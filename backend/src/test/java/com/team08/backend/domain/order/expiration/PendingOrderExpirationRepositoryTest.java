package com.team08.backend.domain.order.expiration;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
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
class PendingOrderExpirationRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void findsOnlyExpiredPendingOrdersWithoutUncertainPayment() {
        LocalDateTime threshold = LocalDateTime.of(2026, 6, 28, 19, 0);
        LocalDateTime oldTime = threshold.minusMinutes(1);

        Order withoutPayment = saveOrder("NO-PAYMENT", oldTime, OrderStatus.PENDING_PAYMENT);
        Order ready = saveOrder("READY", oldTime, OrderStatus.PENDING_PAYMENT);
        savePayment(ready, PaymentStatus.READY, oldTime);
        Order declined = saveOrder("DECLINED", oldTime, OrderStatus.PENDING_PAYMENT);
        savePayment(declined, PaymentStatus.DECLINED, oldTime);

        Order processing = saveOrder("PROCESSING", oldTime, OrderStatus.PENDING_PAYMENT);
        savePayment(processing, PaymentStatus.PROCESSING, oldTime);
        Order unknown = saveOrder("UNKNOWN", oldTime, OrderStatus.PENDING_PAYMENT);
        savePayment(unknown, PaymentStatus.UNKNOWN, oldTime);
        saveOrder("RECENT", threshold.plusMinutes(1), OrderStatus.PENDING_PAYMENT);
        saveOrder("PAID", oldTime, OrderStatus.PAID);

        List<Long> result = orderRepository.findExpirationCandidateIds(
                OrderStatus.PENDING_PAYMENT,
                threshold,
                List.of(PaymentStatus.READY, PaymentStatus.DECLINED),
                PageRequest.of(0, 20)
        );

        assertThat(result).containsExactly(withoutPayment.getId(), ready.getId(), declined.getId());
    }

    private Order saveOrder(String suffix, LocalDateTime orderedAt, OrderStatus status) {
        Order order = Order.createPendingPayment(1L, "ORD-" + suffix, orderedAt);
        if (status == OrderStatus.PAID) {
            order.markPaid(orderedAt.plusSeconds(1));
        }
        return orderRepository.saveAndFlush(order);
    }

    private void savePayment(Order order, PaymentStatus status, LocalDateTime createdAt) {
        Payment payment = Payment.createReady(order, PaymentProviderType.MOCK, createdAt);
        if (status == PaymentStatus.PROCESSING) {
            payment.markProcessing(createdAt);
        } else if (status == PaymentStatus.DECLINED) {
            payment.markProcessing(createdAt);
            payment.decline(null, null, "승인 거절", createdAt);
        } else if (status == PaymentStatus.UNKNOWN) {
            payment.markProcessing(createdAt);
            payment.markUnknown("TIMEOUT", "결제 결과 확인 필요", createdAt);
        }
        paymentRepository.saveAndFlush(payment);
    }
}
