package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategy;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategyFactory;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssuedCouponServiceTest {

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssuedCouponStrategyFactory strategyFactory;

    @InjectMocks
    private IssuedCouponService issuedCouponService;

    @Test
    @DisplayName("성공: 쿠폰 다운로드 요청 시 팩토리를 통해 전략을 가져와 실행한다")
    void downloadCoupon_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        IssuedCouponStrategy strategy = mock(IssuedCouponStrategy.class);
        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.NORMAL);
        when(strategyFactory.getStrategy(CouponType.NORMAL)).thenReturn(strategy);
        when(strategy.issue(userId, policyId)).thenReturn(issuedCoupon);

        // when
        IssuedCouponResponse response = issuedCouponService.downloadCoupon(userId, policyId);

        // then
        assertThat(response).isNotNull();
        verify(strategyFactory, times(1)).getStrategy(CouponType.NORMAL);
        verify(strategy, times(1)).issue(userId, policyId);
    }

    @Test
    @DisplayName("성공: 내 쿠폰 목록을 조회하면 정책 정보와 함께 반환된다")
    void getMyCoupons_success() {
        // given
        Long userId = 1L;
        Long policyId = 10L;
        
        IssuedCoupon coupon = mock(IssuedCoupon.class);
        when(coupon.getPolicyId()).thenReturn(policyId);
        
        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getId()).thenReturn(policyId);
        when(policy.getName()).thenReturn("테스트 쿠폰");

        when(issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(userId)).thenReturn(List.of(coupon));
        when(couponPolicyRepository.findAllById(anyList())).thenReturn(List.of(policy));

        // when
        List<CouponListResponse> responses = issuedCouponService.getMyCoupons(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).couponName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("성공: 쿠폰 적용 시 예상 할인 금액을 정확히 계산한다")
    void calculateExpectedDiscount_success() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 10000;

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        when(issuedCoupon.getUserId()).thenReturn(userId);
        when(issuedCoupon.getStatus()).thenReturn(CouponStatus.ISSUED);
        when(issuedCoupon.getPolicyId()).thenReturn(1L);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getName()).thenReturn("10% 할인 쿠폰");
        when(policy.calculateDiscountAmount(originalPrice)).thenReturn(1000);

        when(issuedCouponRepository.findById(issuedCouponId)).thenReturn(Optional.of(issuedCoupon));
        when(couponPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        ExpectedDiscountResponse response = issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, originalPrice);

        // then
        assertThat(response.discountAmount()).isEqualTo(1000);
        assertThat(response.finalPrice()).isEqualTo(9000);
    }

    @Test
    @DisplayName("성공: 단회성 쿠폰 사용 시 상태가 USED로 변경된다")
    void useCouponForOrder_singleUse_success() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 10000;

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        when(issuedCoupon.getUserId()).thenReturn(userId);
        when(issuedCoupon.getStatus()).thenReturn(CouponStatus.ISSUED);
        when(issuedCoupon.getPolicyId()).thenReturn(1L);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getUsageType()).thenReturn(com.team08.backend.domain.couponpolicy.entity.CouponUsageType.SINGLE_USE);
        when(policy.calculateDiscountAmount(originalPrice)).thenReturn(1000);

        when(issuedCouponRepository.findById(issuedCouponId)).thenReturn(Optional.of(issuedCoupon));
        when(couponPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        int discountAmount = issuedCouponService.useCouponForOrder(userId, issuedCouponId, originalPrice);

        // then
        assertThat(discountAmount).isEqualTo(1000);
        verify(issuedCoupon).use();
    }
}
