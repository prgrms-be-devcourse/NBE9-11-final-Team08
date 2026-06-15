package com.team08.backend.domain.issuedcoupon.entity;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IssuedCouponTest {

    @Test
    @DisplayName("실패: 쿠폰 상태가 ISSUED여도 만료 시간이 지났다면 예외가 발생한다 (실시간 만료 체크 검증)")
    void validateUsable_fail_expiredTime() {
        // given
        Long userId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        LocalDateTime now = LocalDateTime.now();

        when(policy.calculateExpirationDate()).thenReturn(LocalDateTime.now().plusDays(30));
        IssuedCoupon coupon = IssuedCoupon.create(policy, userId, now);

        ReflectionTestUtils.setField(coupon, "expiredAt", now.minusSeconds(1));

        // when & then
        assertThatThrownBy(() -> coupon.validateUsable(userId, now))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_USED_OR_EXPIRED);
    }

    @Test
    @DisplayName("실패: 본인의 쿠폰이 아니면 예외가 발생한다")
    void validateUsable_fail_notOwned() {
        // given
        Long ownerId = 1L;
        Long otherUserId = 2L;
        CouponPolicy policy = mock(CouponPolicy.class);
        LocalDateTime now = LocalDateTime.now();
        when(policy.calculateExpirationDate()).thenReturn(now.plusDays(30));
        
        IssuedCoupon coupon = IssuedCoupon.create(policy, ownerId, now);

        // when & then
        assertThatThrownBy(() -> coupon.validateUsable(otherUserId, now))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_OWNED);
    }
}
