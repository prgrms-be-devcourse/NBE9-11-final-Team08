package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IssuedCouponWriter {

    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IssuedCoupon saveWithConcurrencyProtection(IssuedCoupon coupon) {
        try {
            return issuedCouponRepository.saveAndFlush(coupon);
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyIssuedException();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public java.util.List<IssuedCoupon> saveAllWithConcurrencyProtection(java.util.List<IssuedCoupon> coupons) {
        try {
            return issuedCouponRepository.saveAllAndFlush(coupons);
        } catch (DataIntegrityViolationException e) {
            throw new CouponAlreadyIssuedException();
        }
    }
}
