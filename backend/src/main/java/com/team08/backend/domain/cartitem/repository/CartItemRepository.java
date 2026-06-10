package com.team08.backend.domain.cartitem.repository;

import com.team08.backend.domain.cartitem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
