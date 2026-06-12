package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
    // 특정 사용자에게 해당 쿠폰이 이미 발급되었는지 확인
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);
}
