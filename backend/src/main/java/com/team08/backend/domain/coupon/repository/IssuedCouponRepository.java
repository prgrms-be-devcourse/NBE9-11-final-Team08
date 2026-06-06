package com.team08.backend.domain.coupon.repository;

import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    long countByCouponId(Long couponId);
}
