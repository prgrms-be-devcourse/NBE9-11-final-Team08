package com.team08.backend.domain.couponpolicycategory.repository;

import com.team08.backend.domain.couponpolicycategory.entity.CouponPolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyCategoryRepository extends JpaRepository<CouponPolicyCategory, Long> {
}
