package com.team08.backend.domain.payment.repository;

import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_Id(Long orderId);

    Optional<Payment> findByProviderAndOrder_OrderNumber(PaymentProviderType provider, String orderNumber);

    @Query("""
            select p
            from Payment p
            join fetch p.order o
            where p.provider = :provider
              and p.status in :statuses
              and coalesce(p.updatedAt, p.createdAt) < :threshold
            order by coalesce(p.updatedAt, p.createdAt) asc
            """)
    List<Payment> findRecoverablePayments(
            @Param("provider") PaymentProviderType provider,
            @Param("statuses") Collection<PaymentStatus> statuses,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable
    );
}
