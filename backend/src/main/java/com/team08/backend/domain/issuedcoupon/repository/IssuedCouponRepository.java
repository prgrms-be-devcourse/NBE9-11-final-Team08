package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
}
