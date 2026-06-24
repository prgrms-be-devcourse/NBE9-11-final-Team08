package com.team08.backend.domain.payment.repository;

import com.team08.backend.domain.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    List<PaymentAttempt> findAllByPayment_IdOrderByCreatedAtAsc(Long paymentId);

    Optional<PaymentAttempt> findByPayment_IdAndIdempotencyKey(Long paymentId, String idempotencyKey);
}
