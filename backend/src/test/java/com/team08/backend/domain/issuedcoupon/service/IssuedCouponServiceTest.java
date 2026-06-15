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
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @InjectMocks
    private IssuedCouponService issuedCouponService;

    @Test
    @DisplayName("성공: 일반 쿠폰 다운로드 요청 시 정상적으로 발급된다")
    void downloadCoupon_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.NORMAL);
        when(policy.getId()).thenReturn(policyId);

        // 중복 체크 통과
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);

        when(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        IssuedCouponResponse response = issuedCouponService.downloadCoupon(userId, policyId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.policyId()).isEqualTo(policyId);
        verify(issuedCouponRepository, times(1)).saveAndFlush(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("실패: 일반 쿠폰 다운로드 시 DB 유니크 제약 위반(동시성) 발생 시 예외 발생")
    void downloadCoupon_fail_dataIntegrityViolation() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.NORMAL);
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);

        // saveAndFlush 시점에 예외 발생 (동시성 상황 모사)
        when(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadCoupon(userId, policyId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("실패: 일반 쿠폰 이미 발급받은 경우 예외 발생")
    void downloadCoupon_fail_alreadyIssued() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.NORMAL);

        // 중복 체크 걸림
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            issuedCouponService.downloadCoupon(userId, policyId);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("실패: 일반 쿠폰 발급 기간 전일 때 예외 발생")
    void downloadCoupon_fail_notStarted() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.NORMAL);
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);

        // 기간 검증 예외 발생 모사
        doThrow(new CustomException(ErrorCode.COUPON_ISSUE_PERIOD_NOT_STARTED)).when(policy).validateIssuePeriod();

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadCoupon(userId, policyId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ISSUE_PERIOD_NOT_STARTED);
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 재고가 1개일 때 마지막 사람이 성공하고 재고가 0이 된다")
    void downloadFcfsCoupon_success_lastOne() {
        // given
        Long userId = 1L;
        Long policyId = 100L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.FCFS);
        when(policy.getId()).thenReturn(policyId);
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);

        when(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        issuedCouponService.downloadFcfsCoupon(userId, policyId);

        // then
        verify(policy, times(1)).decreaseQuantity();
        verify(issuedCouponRepository, times(1)).saveAndFlush(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("실패: 재고가 0일 때 선착순 쿠폰 발급 요청 시 예외가 발생한다")
    void downloadFcfsCoupon_fail_soldOut() {
        // given
        Long userId = 1L;
        Long policyId = 100L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.FCFS);
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);

        // decreaseQuantity에서 예외 발생 모사
        doThrow(new CustomException(ErrorCode.COUPON_EXHAUSTED)).when(policy).decreaseQuantity();

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            issuedCouponService.downloadFcfsCoupon(userId, policyId);
        });
        assertEquals(ErrorCode.COUPON_EXHAUSTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패: 선착순 쿠폰 이미 발급받은 유저가 요청하면 중복 발급 예외가 발생한다")
    void downloadFcfsCoupon_fail_duplicated() {
        // given
        Long userId = 1L;
        Long policyId = 100L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findByIdWithLock(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.FCFS);

        // 중복 체크 걸림
        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadFcfsCoupon(userId, policyId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("성공: 내 쿠폰 목록을 조회하면 정책 정보와 함께 반환된다")
    void getMyCoupons_success() {
        // given
        Long userId = 1L;
        Long policyId = 10L;
        
        IssuedCoupon coupon = mock(IssuedCoupon.class);
        when(coupon.getPolicyId()).thenReturn(policyId);
        when(coupon.getId()).thenReturn(100L);
        
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
        verify(issuedCouponRepository).findByUserIdOrderByExpiredAtAsc(userId);
        verify(couponPolicyRepository).findAllById(anyList());
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
