package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcfsIssuedCouponStrategyTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    private Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private FcfsIssuedCouponStrategy fcfsIssuedCouponStrategy;

    @BeforeEach
    void setUp() {
        fcfsIssuedCouponStrategy = new FcfsIssuedCouponStrategy(
                couponPolicyRepository,
                issuedCouponRepository,
                clock
        );
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 전략이 정상적으로 재고를 차감하고 쿠폰을 생성하여 반환한다")
    void issue_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        LocalDateTime now = LocalDateTime.now(clock);

        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);
        when(policy.getId()).thenReturn(policyId);
        when(policy.calculateExpirationDate(any(LocalDateTime.class))).thenReturn(now.plusDays(30));

        // when
        IssuedCoupon result = fcfsIssuedCouponStrategy.issue(userId, policyId);

        // then
        assertThat(result).isNotNull();
        verify(policy).decreaseQuantity();
    }

    @Test
    @DisplayName("실패: 재고 부족 시 예외가 발생한다")
    void issue_fail_soldOut() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        doThrow(new CouponExhaustedException()).when(policy).decreaseQuantity();

        // when & then
        assertThatThrownBy(() -> fcfsIssuedCouponStrategy.issue(userId, policyId))
                .isInstanceOf(CouponExhaustedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);
    }
}
