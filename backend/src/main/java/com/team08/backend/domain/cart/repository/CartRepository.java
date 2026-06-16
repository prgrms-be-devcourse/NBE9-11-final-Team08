package com.team08.backend.domain.cart.repository;

import com.team08.backend.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    @Query("select distinct c from Cart c left join fetch c.items where c.userId = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
}
