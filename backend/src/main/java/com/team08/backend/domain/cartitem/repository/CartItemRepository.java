package com.team08.backend.domain.cartitem.repository;

import com.team08.backend.domain.cartitem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("select ci from CartItem ci where ci.cart.id = :cartId")
    List<CartItem> findAllByCartId(@Param("cartId") Long cartId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem ci where ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}
