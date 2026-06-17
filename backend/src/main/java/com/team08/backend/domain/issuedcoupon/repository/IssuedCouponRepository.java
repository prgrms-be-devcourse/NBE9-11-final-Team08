package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
    // 특정 사용자에게 해당 쿠폰이 이미 발급되었는지 확인
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    // 사용자의 쿠폰 목록을 만료일 임박순으로 조회
    List<IssuedCoupon> findByUserIdOrderByExpiredAtAsc(Long userId);

    // [벌크] 만료일이 지난 미사용 쿠폰을 만료(EXPIRED) 상태로 한 번에 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE IssuedCoupon c SET c.status = :expiredStatus WHERE c.status = :issuedStatus AND c.expiredAt < CURRENT_TIMESTAMP")
    int expirePastCoupons(
            @Param("issuedStatus") CouponStatus issuedStatus,
            @Param("expiredStatus") CouponStatus expiredStatus
    );
    
    // 특정 정책으로부터 발급된 쿠폰 수 조회
    long countByPolicyId(Long policyId);
}
