package com.team08.backend.domain.order.expiration;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PendingOrderExpirationService {

    private static final List<PaymentStatus> EXPIRABLE_PAYMENT_STATUSES = List.of(
            PaymentStatus.READY,
            PaymentStatus.DECLINED
    );

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PendingOrderExpirationProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Long> findExpirationCandidateIds() {
        LocalDateTime threshold = expirationThreshold();
        return orderRepository.findExpirationCandidateIds(
                OrderStatus.PENDING_PAYMENT,
                threshold,
                EXPIRABLE_PAYMENT_STATUSES,
                PageRequest.of(0, properties.batchSize())
        );
    }

    @Transactional
    public boolean expireOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || !isExpiredPendingOrder(order)) {
            return false;
        }

        Optional<Payment> payment = paymentRepository.findByOrder_Id(orderId);
        if (payment.isPresent() && !EXPIRABLE_PAYMENT_STATUSES.contains(payment.get().getStatus())) {
            return false;
        }

        LocalDateTime expiredAt = LocalDateTime.now(clock);
        payment.ifPresent(existingPayment -> existingPayment.cancel(expiredAt));
        order.expire(expiredAt);
        return true;
    }

    private boolean isExpiredPendingOrder(Order order) {
        return order.getStatus() == OrderStatus.PENDING_PAYMENT
                && !order.getOrderedAt().isAfter(expirationThreshold());
    }

    private LocalDateTime expirationThreshold() {
        return LocalDateTime.now(clock).minusMinutes(properties.expirationMinutes());
    }
}
