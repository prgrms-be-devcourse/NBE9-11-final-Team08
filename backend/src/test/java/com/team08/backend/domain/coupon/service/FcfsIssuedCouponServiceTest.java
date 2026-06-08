package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.entity.CouponType;
import com.team08.backend.domain.coupon.repository.CouponPolicyRepository;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcfsIssuedCouponServiceTest {

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IssuedCouponService issuedCouponService;
    
    @Test
    @DisplayName("성공: 선착순 쿠폰 재고가 1개일 때 마지막 사람이 성공하고 재고가 0이 된다")
    void downloadFcfsCoupon_success_lastOne() {
        // given
        Long userId = 1L;
        Long policyId = 100L;
        CouponPolicy policy = CouponPolicy.builder()
                .totalQuantity(1)
                .validDays(7)
                .couponType(CouponType.FCFS) // [추가됨] 타입 통과용
                .issueStartDate(LocalDateTime.now().minusDays(1)) // [추가됨] 기간 통과용
                .issueEndDate(LocalDateTime.now().plusDays(1))
                .build();
        User mockUser = User.builder().build();

        given(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(couponPolicyRepository.findByIdWithLock(policyId)).willReturn(Optional.of(policy));

        // when
        issuedCouponService.downloadFcfsCoupon(userId, policyId);

        // then
        assertEquals(0, policy.getTotalQuantity()); // 재고가 0이 되었는지 확인
        verify(issuedCouponRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("실패: 재고가 0일 때 선착순 쿠폰 발급 요청 시 예외가 발생한다")
    void downloadFcfsCoupon_fail_soldOut() {
        // given
        Long userId = 1L;
        Long policyId = 100L;
        CouponPolicy policy = CouponPolicy.builder()
                .totalQuantity(0)
                .validDays(7)
                .couponType(CouponType.FCFS) // [추가됨] 타입 통과용
                .issueStartDate(LocalDateTime.now().minusDays(1)) // [추가됨] 기간 통과용
                .issueEndDate(LocalDateTime.now().plusDays(1))
                .build();
        User mockUser = User.builder().build();

        given(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(couponPolicyRepository.findByIdWithLock(policyId)).willReturn(Optional.of(policy));

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            issuedCouponService.downloadFcfsCoupon(userId, policyId);
        });
    }

    // ==========================================
    // 2. 엣지 케이스 검증 테스트 (새로 다듬은 코드)
    // ==========================================

    @Test
    @DisplayName("실패: 쿠폰 발급 시작 시간 이전에 요청하면 예외가 발생한다.")
    void downloadFcfsCoupon_Fail_BeforeStartDate() {
        // given
        Long userId = 1L;
        Long policyId = 100L;

        User user = User.builder().id(userId).build();
        CouponPolicy policy = CouponPolicy.builder()
                .couponType(CouponType.FCFS)
                .issueStartDate(LocalDateTime.now().plusHours(1)) // 1시간 뒤에 오픈됨!
                .build();

        given(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(couponPolicyRepository.findByIdWithLock(policyId)).willReturn(Optional.of(policy));

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadFcfsCoupon(userId, policyId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("아직 쿠폰 발급 기간이 시작되지 않았습니다.");
    }

    @Test
    @DisplayName("실패: 이미 발급받은 유저가 요청하면 중복 발급 예외가 발생한다.")
    void downloadFcfsCoupon_Fail_Duplicated() {
        // given
        Long userId = 1L;
        Long policyId = 100L;

        // 이미 발급받았다고 모킹
        given(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadFcfsCoupon(userId, policyId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");
    }

    @Test
    @DisplayName("실패: 선착순 전용 쿠폰이 아닌 다른 타입(NORMAL 등)을 요청하면 예외가 발생한다.")
    void downloadFcfsCoupon_Fail_NotFcfsType() {
        // given
        Long userId = 1L;
        Long policyId = 100L;

        User user = User.builder().id(userId).build();
        CouponPolicy policy = CouponPolicy.builder()
                .couponType(CouponType.NORMAL) // 선착순 타입이 아님!
                .issueStartDate(LocalDateTime.now().minusDays(1))
                .issueEndDate(LocalDateTime.now().plusDays(1))
                .build();

        given(issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(couponPolicyRepository.findByIdWithLock(policyId)).willReturn(Optional.of(policy));

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadFcfsCoupon(userId, policyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선착순 발급 전용 쿠폰이 아닙니다.");
    }
}
