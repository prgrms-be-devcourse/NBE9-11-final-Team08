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
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    List<IssuedCoupon> findByUserIdOrderByExpiredAtAsc(Long userId);

    long countByPolicyId(Long policyId);

    Optional<IssuedCoupon> findByUserIdAndPolicyId(Long userId, Long policyId);

    @Query("""
            SELECT c.userId
            FROM IssuedCoupon c
            WHERE c.policyId = :policyId
              AND c.userId IN :userIds
            """)
    List<Long> findIssuedUserIds(@Param("policyId") Long policyId, @Param("userIds") List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM IssuedCoupon c WHERE c.id = :id")
    Optional<IssuedCoupon> findByIdWithLock(@Param("id") Long id);
}
