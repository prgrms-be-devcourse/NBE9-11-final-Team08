package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcfsIssuedCouponStrategyTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    private FcfsIssuedCouponStrategy fcfsIssuedCouponStrategy;

    @Test
    @DisplayName("성공: 선착순 쿠폰 전략이 정상적으로 재고를 차감하고 발급한다")
    void issue_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        LocalDateTime now = LocalDateTime.now();

        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);
        when(policy.getId()).thenReturn(policyId);
        when(policy.calculateExpirationDate()).thenReturn(now.plusDays(30));
        when(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        IssuedCoupon result = fcfsIssuedCouponStrategy.issue(userId, policyId);

        // then
        assertThat(result).isNotNull();
        verify(policy).decreaseQuantity();
        verify(issuedCouponRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("실패: 재고 부족 시 예외가 발생한다")
    void issue_fail_soldOut() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        doThrow(new CustomException(ErrorCode.COUPON_EXHAUSTED)).when(policy).decreaseQuantity();

        // when & then
        assertThatThrownBy(() -> fcfsIssuedCouponStrategy.issue(userId, policyId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);
    }
}
