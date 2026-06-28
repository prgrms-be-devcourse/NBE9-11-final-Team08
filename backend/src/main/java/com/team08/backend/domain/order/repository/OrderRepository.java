package com.team08.backend.domain.order.repository;

import com.team08.backend.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Collection;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserIdOrderByOrderedAtDescIdDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.userId = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            select o.id
            from Order o
            where o.status = :orderStatus
              and o.orderedAt <= :threshold
              and (
                    not exists (select p.id from Payment p where p.order = o)
                    or exists (
                        select p.id
                        from Payment p
                        where p.order = o
                          and p.status in :paymentStatuses
                    )
              )
            order by o.orderedAt asc, o.id asc
            """)
    List<Long> findExpirationCandidateIds(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("threshold") LocalDateTime threshold,
            @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
