package com.team08.backend.domain.coupon.repository;

import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    // 특정 사용자에게 해당 쿠폰이 이미 발급되었는지 확인 (중복 발급 검증용)
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    // [벌크] 만료일이 지난 미사용 쿠폰을 만료(EXPIRED) 상태로 한 번에 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE IssuedCoupon c SET c.status = :expiredStatus WHERE c.status = :issuedStatus AND c.expiredAt < :now")
    int expirePastCoupons(
            @Param("now") LocalDateTime now,
            @Param("issuedStatus") CouponStatus issuedStatus,
            @Param("expiredStatus") CouponStatus expiredStatus
    );

    // 사용자 보유 쿠폰 목록 조회 (최신 발급순 정렬)
    List<IssuedCoupon> findByUserIdOrderByIssuedAtDesc(Long userId);

}
