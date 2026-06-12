package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        
        when(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        IssuedCouponResponse response = issuedCouponService.downloadCoupon(userId, policyId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.policyId()).isEqualTo(policyId);
        verify(issuedCouponRepository, times(1)).saveAndFlush(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("실패: 이미 발급받은 쿠폰인 경우 예외가 발생한다")
    void downloadCoupon_fail_alreadyIssued() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(policy.getCouponType()).thenReturn(CouponType.NORMAL);

        doThrow(DataIntegrityViolationException.class).when(issuedCouponRepository).saveAndFlush(any(IssuedCoupon.class));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            issuedCouponService.downloadCoupon(userId, policyId);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
    }
}
