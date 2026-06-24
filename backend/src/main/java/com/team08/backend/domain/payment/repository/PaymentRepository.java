package com.team08.backend.domain.payment.repository;

import com.team08.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_Id(Long orderId);
}
