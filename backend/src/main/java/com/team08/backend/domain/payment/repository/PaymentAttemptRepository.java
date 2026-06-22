package com.team08.backend.domain.payment.repository;

import com.team08.backend.domain.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    List<PaymentAttempt> findAllByPayment_IdOrderByCreatedAtAsc(Long paymentId);
}
