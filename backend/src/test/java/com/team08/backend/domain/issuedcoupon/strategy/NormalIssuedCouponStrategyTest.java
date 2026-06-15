package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.global.exception.CustomException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalIssuedCouponStrategyTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    private Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private NormalIssuedCouponStrategy normalIssuedCouponStrategy;

    @BeforeEach
    void setUp() {
        normalIssuedCouponStrategy = new NormalIssuedCouponStrategy(
                couponPolicyRepository,
                issuedCouponRepository,
                clock
        );
    }

    @Test
    @DisplayName("성공: 일반 쿠폰 전략이 정상적으로 쿠폰을 생성하여 반환한다")
    void issue_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        LocalDateTime now = LocalDateTime.now(clock);

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);
        when(policy.getId()).thenReturn(policyId);
        when(policy.calculateExpirationDate()).thenReturn(now.plusDays(30));

        // when
        IssuedCoupon result = normalIssuedCouponStrategy.issue(userId, policyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPolicyId()).isEqualTo(policyId);
        verify(policy).validateIssuePeriod();
    }

    @Test
    @DisplayName("실패: 중복 발급 시 예외가 발생한다")
    void issue_fail_duplicate() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> normalIssuedCouponStrategy.issue(userId, policyId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);
    }
}
