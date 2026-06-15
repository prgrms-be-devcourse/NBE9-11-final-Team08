package com.team08.backend.domain.issuedcoupon.repository;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IssuedCouponWriter {

    private final IssuedCouponRepository issuedCouponRepository;

    // 중복 발급 에러가 부모 트랜잭션을 롤백시키지 않도록 독립 트랜잭션으로 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IssuedCoupon saveWithConcurrencyProtection(IssuedCoupon coupon) {
        try {
            return issuedCouponRepository.saveAndFlush(coupon);
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyIssuedException();
        }
    }
}
