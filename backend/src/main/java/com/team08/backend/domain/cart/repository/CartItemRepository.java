package com.team08.backend.domain.cart.repository;

import com.team08.backend.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByCartUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    boolean existsByCartIdAndCourseId(Long cartId, Long courseId);

    void deleteAllByCartUserId(Long userId);
}
