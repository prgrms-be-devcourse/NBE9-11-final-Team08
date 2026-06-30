package com.team08.backend.domain.ordercouponusage.repository;

import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderCouponUsageRepository extends JpaRepository<OrderCouponUsage, Long> {
    Optional<OrderCouponUsage> findByOrderId(Long orderId);
}
