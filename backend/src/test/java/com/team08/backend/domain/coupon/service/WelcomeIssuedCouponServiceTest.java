package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.entity.CouponTarget;
import com.team08.backend.domain.coupon.entity.CouponType;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import com.team08.backend.domain.coupon.repository.CouponPolicyRepository;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomeIssuedCouponServiceTest {

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IssuedCouponService issuedCouponService;

    @Test
    @DisplayName("신규 회원 가입 시 WELCOME 쿠폰이 정상적으로 발급된다")
    void issueSignUpCoupon_success() {
        // given
        Long userId = 1L;
        User mockUser = User.builder().build();
        CouponPolicy welcomePolicy = CouponPolicy.builder()
                .name("신규가입 쿠폰")
                .couponType(CouponType.AUTO)
                .couponTarget(CouponTarget.ALL)
                .validDays(7)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(couponPolicyRepository.findByCouponType(CouponType.AUTO)).thenReturn(Optional.of(welcomePolicy));

        // when
        issuedCouponService.issueSignUpCoupon(userId);

        // then
        verify(userRepository).findById(userId);
        verify(couponPolicyRepository).findByCouponType(CouponType.AUTO);

        ArgumentCaptor<IssuedCoupon> captor = ArgumentCaptor.forClass(IssuedCoupon.class);
        verify(issuedCouponRepository).save(captor.capture());

        IssuedCoupon savedCoupon = captor.getValue();

        assertNotNull(savedCoupon.getExpiredAt());
        assertEquals(mockUser, savedCoupon.getUser());
        assertEquals(welcomePolicy, savedCoupon.getPolicy());
        assertEquals(LocalDate.now().plusDays(7).atTime(LocalTime.MAX), savedCoupon.getExpiredAt());
    }

    @Test
    @DisplayName("유저가 특정 쿠폰을 정상적으로 다운로드 받는다")
    void downloadCoupon_success() {
        // given
        Long userId = 1L;
        Long policyId = 10L;
        User mockUser = User.builder().build();
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .name("작가의 날 기념 도서 10% 할인 쿠폰")
                .couponTarget(CouponTarget.BOOK)
                .couponType(CouponType.NORMAL) // 이 부분을 추가해야 합니다
                .validDays(7)
                .build();

        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(mockPolicy));

        // when
        issuedCouponService.downloadCoupon(userId, policyId);

        // then
        verify(issuedCouponRepository, times(1)).save(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("이미 다운로드 받은 쿠폰을 다시 요청하면 예외가 발생한다")
    void downloadCoupon_duplicated_throwsException() {
        // given
        Long userId = 1L;
        Long policyId = 10L;

        when(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(true);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            issuedCouponService.downloadCoupon(userId, policyId);
        });

        assertEquals("이미 발급받은 쿠폰입니다.", exception.getMessage());

        verify(userRepository, never()).findById(anyLong());
        verify(issuedCouponRepository, never()).save(any());
    }
}
