package com.team08.backend.domain.couponpolicy.repository;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
}
