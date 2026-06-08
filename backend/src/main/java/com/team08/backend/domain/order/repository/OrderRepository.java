package com.team08.backend.domain.order.repository;

import com.team08.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByUserIdOrderByOrderedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
