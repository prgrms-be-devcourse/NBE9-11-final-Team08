package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.repository.CouponRepository;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponIssueServiceTest {

    @InjectMocks
    private CouponIssueService couponIssueService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Test
    @DisplayName("성공: 오전 10시 이후에 선착순 쿠폰을 요청하면 정상 발급된다.")
    void issueCoupon_Success() {
        // Given
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime requestTime = LocalDateTime.of(2026, 10, 9, 10, 5, 0); // 10시 5분

        given(issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(false);
        given(issuedCouponRepository.countByCouponId(couponId)).willReturn(999L); // 999명 발급됨

        // When
        couponIssueService.issueFirstComeCoupon(userId, couponId, requestTime);

        // Then
        verify(issuedCouponRepository).save(any()); // 저장 로직이 호출되었는지 검증
    }

    @Test
    @DisplayName("실패: 오전 10시 이전에 요청하면 예외가 발생한다.")
    void issueCoupon_Fail_Before10AM() {
        // Given
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime requestTime = LocalDateTime.of(2026, 10, 9, 9, 59, 59); // 9시 59분

        // When & Then
        assertThatThrownBy(() -> couponIssueService.issueFirstComeCoupon(userId, couponId, requestTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("쿠폰 발급은 오전 10시부터 가능합니다.");
    }

    @Test
    @DisplayName("실패: 이미 발급받은 유저가 요청하면 중복 발급 예외가 발생한다.")
    void issueCoupon_Fail_Duplicated() {
        // Given
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime requestTime = LocalDateTime.of(2026, 10, 9, 10, 5, 0);

        // 이미 발급받았다고 모킹
        given(issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> couponIssueService.issueFirstComeCoupon(userId, couponId, requestTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");
    }

    @Test
    @DisplayName("실패: 1000개가 모두 소진된 후 요청하면 예외가 발생한다.")
    void issueCoupon_Fail_SoldOut() {
        // Given
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime requestTime = LocalDateTime.of(2026, 10, 9, 10, 5, 0);

        given(issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(false);
        // 이미 1000개가 발급되었다고 모킹
        given(issuedCouponRepository.countByCouponId(couponId)).willReturn(1000L);

        // When & Then
        assertThatThrownBy(() -> couponIssueService.issueFirstComeCoupon(userId, couponId, requestTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("선착순 쿠폰이 모두 소진되었습니다.");
    }
}
