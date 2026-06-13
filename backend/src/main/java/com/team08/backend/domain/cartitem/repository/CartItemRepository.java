package com.team08.backend.domain.cartitem.repository;

import com.team08.backend.domain.cartitem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByCartId(Long cartId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    boolean existsByCartIdAndCourseId(Long cartId, Long courseId);

    void deleteAllByCartId(Long cartId);
}
