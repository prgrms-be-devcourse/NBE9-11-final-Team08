package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.exception.CouponExhaustedException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.exception.CouponIssueFailedException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.service.FcfsCouponRedisIssuer;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobStreamPublisher;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcfsIssuedCouponStrategyTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private FcfsCouponRedisIssuer fcfsCouponRedisIssuer;

    @Mock
    private IssuedCouponJobStreamPublisher issuedCouponJobStreamPublisher;

    private Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private FcfsIssuedCouponStrategy fcfsIssuedCouponStrategy;

    @BeforeEach
    void setUp() {
        fcfsIssuedCouponStrategy = new FcfsIssuedCouponStrategy(
                clock,
                couponPolicyRepository,
                fcfsCouponRedisIssuer,
                issuedCouponJobStreamPublisher
        );
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 전략이 정상적으로 재고를 차감하고 요청 상태 결과를 반환한다")
    void issue_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        LocalDateTime now = LocalDateTime.now(clock);

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getId()).thenReturn(policyId);

        // when
        CouponIssueResult result = fcfsIssuedCouponStrategy.issue(userId, policyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(CouponIssueResult.Status.REQUESTED);
        verify(couponPolicyRepository).findById(policyId);
        verify(couponPolicyRepository, never()).findByIdWithLock(policyId);
        verify(issuedCouponRepository, never()).existsByUserIdAndPolicyId(userId, policyId);
        verify(fcfsCouponRedisIssuer).issue(userId, policy);
        verify(issuedCouponJobStreamPublisher).publish(any(String.class), any(Long.class), any(Long.class));
        verify(policy, never()).decreaseQuantity();
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 Stream 적재 실패 시 예외를 전파하고 Redis 선점을 롤백한다")
    void issue_streamPublishFail_throwExceptionAndRollback() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getId()).thenReturn(policyId);
        doThrow(new IllegalStateException("stream")).when(issuedCouponJobStreamPublisher)
                .publish(any(String.class), any(Long.class), any(Long.class));

        // when & then
        assertThatThrownBy(() -> fcfsIssuedCouponStrategy.issue(userId, policyId))
                .isInstanceOf(CouponIssueFailedException.class);
                
        verify(fcfsCouponRedisIssuer).issue(userId, policy);
        verify(fcfsCouponRedisIssuer).rollback(userId, policyId);
    }

    @Test
    @DisplayName("실패: 재고 부족 시 예외가 발생한다")
    void issue_fail_soldOut() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        doThrow(new CouponExhaustedException()).when(fcfsCouponRedisIssuer).issue(userId, policy);

        // when & then
        assertThatThrownBy(() -> fcfsIssuedCouponStrategy.issue(userId, policyId))
                .isInstanceOf(CouponExhaustedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);
                
        verify(issuedCouponJobStreamPublisher, never()).publish(any(String.class), any(Long.class), any(Long.class));
    }
}
