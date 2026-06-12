package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
    // 특정 사용자에게 해당 쿠폰이 이미 발급되었는지 확인
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    // 사용자의 쿠폰 목록을 만료일 임박순으로 조회
    List<IssuedCoupon> findByUserIdOrderByExpiredAtAsc(Long userId);
}
