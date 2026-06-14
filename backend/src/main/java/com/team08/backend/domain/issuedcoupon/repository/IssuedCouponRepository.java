package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
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

    // 쿠폰 발급 저장 및 동시성 방어
    default IssuedCoupon saveWithConcurrencyProtection(IssuedCoupon coupon) {
        try {
            return saveAndFlush(coupon);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

}
