package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
    // 특정 사용자에게 해당 쿠폰이 이미 발급되었는지 확인
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    // 사용자의 쿠폰 목록을 만료일 임박순으로 조회
    List<IssuedCoupon> findByUserIdOrderByExpiredAtAsc(Long userId);

    // 특정 정책으로부터 발급된 쿠폰 수 조회
    long countByPolicyId(Long policyId);

    // 특정 사용자에게 발급된 특정 쿠폰 조회
    Optional<IssuedCoupon> findByUserIdAndPolicyId(Long userId, Long policyId);

    @Query("""
            SELECT c.userId
            FROM IssuedCoupon c
            WHERE c.policyId = :policyId
              AND c.userId IN :userIds
            """)
    List<Long> findIssuedUserIds(@Param("policyId") Long policyId, @Param("userIds") List<Long> userIds);

    // 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM IssuedCoupon c WHERE c.id = :id")
    Optional<IssuedCoupon> findByIdWithLock(@Param("id") Long id);
}
