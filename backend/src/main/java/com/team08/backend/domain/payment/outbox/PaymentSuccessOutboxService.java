package com.team08.backend.domain.payment.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentSuccessOutboxService {

    private final PaymentSuccessOutboxRepository paymentSuccessOutboxRepository;

    public void createIfAbsent(Long paymentId, Long orderId, Long userId) {
        if (paymentSuccessOutboxRepository.existsByPaymentId(paymentId)) {
            return;
        }

        paymentSuccessOutboxRepository.save(
                PaymentSuccessOutboxEvent.paymentSucceeded(paymentId, orderId, userId)
        );
    }
}
