package com.team08.backend.domain.payment.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentSuccessOutboxRepository extends JpaRepository<PaymentSuccessOutboxEvent, Long> {

    boolean existsByPaymentId(Long paymentId);

    @Query("""
            select event
            from PaymentSuccessOutboxEvent event
            where event.status = :pendingStatus
               or (event.status = :failedStatus and event.nextRetryAt <= :now)
            order by event.id asc
            """)
    List<PaymentSuccessOutboxEvent> findReady(
            @Param("pendingStatus") PaymentSuccessOutboxStatus pendingStatus,
            @Param("failedStatus") PaymentSuccessOutboxStatus failedStatus,
            @Param("now") java.time.LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from PaymentSuccessOutboxEvent event where event.id = :id")
    Optional<PaymentSuccessOutboxEvent> findByIdForUpdate(@Param("id") Long id);
}
