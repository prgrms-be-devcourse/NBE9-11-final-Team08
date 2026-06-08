package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.entity.DiscountType;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CouponUsageTest {

    @InjectMocks
    private IssuedCouponService issuedCouponService;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;
    
    @Test
    @DisplayName("예상 할인 금액 조회 성공 - 정액 할인")
    void calculateExpectedDiscount_Success() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 50000;

        User user = User.create("test@test.com", "password", "test");
        ReflectionTestUtils.setField(user, "id", userId);

        CouponPolicy policy = CouponPolicy.builder()
                .name("여름 휴가 3천원 할인")
                .discountType(DiscountType.AMOUNT)
                .discountValue(3000)
                .validDays(7)
                .build();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.of(issuedCoupon));

        // when
        ExpectedDiscountResponse response = issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, originalPrice);

        // then
        assertThat(response.couponName()).isEqualTo("여름 휴가 3천원 할인");
        assertThat(response.originalPrice()).isEqualTo(50000);
        assertThat(response.discountAmount()).isEqualTo(3000);
        assertThat(response.finalPrice()).isEqualTo(47000);
    }

    @Test
    @DisplayName("타인의 쿠폰으로 조회 시 예외 발생")
    void calculateExpectedDiscount_Fail_NotOwner() {
        // given
        Long requesterId = 1L;
        Long ownerId = 2L; // 소유자가 다름
        Long issuedCouponId = 100L;

        User owner = User.create("owner@test.com", "password", "owner");
        ReflectionTestUtils.setField(owner, "id", ownerId);
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .user(owner)
                .build();

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.of(issuedCoupon));

        // when & then
        assertThatThrownBy(() -> issuedCouponService.calculateExpectedDiscount(requesterId, issuedCouponId, 50000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인의 쿠폰만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("예상 할인 금액 조회 실패 - 이미 사용된 쿠폰(USED)일 경우 예외가 발생한다")
    void calculateExpectedDiscount_WhenCouponIsUsed_ThrowsException() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;

        User user = User.create("test@test.com", "password", "test");
        ReflectionTestUtils.setField(user, "id", userId);
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1)) // 사용(use) 처리를 위해 만료일 세팅
                .build();
        
        issuedCoupon.use(); // 객체의 행위를 통해 USED 상태로 변경!

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.of(issuedCoupon));

        // when & then
        assertThatThrownBy(() -> issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, 50000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 수 없는 쿠폰 상태입니다.");
    }

    @Test
    @DisplayName("결제 시 쿠폰 사용 성공 (상태 변경 확인)")
    void useCouponForOrder_Success() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 50000;

        User user = User.create("test@test.com", "password", "test");
        ReflectionTestUtils.setField(user, "id", userId);

        CouponPolicy policy = CouponPolicy.builder()
                .name("10% 할인 쿠폰")
                .discountType(DiscountType.PERCENT)
                .discountValue(10)
                .maxDiscountAmount(10000) // 최대 1만원 할인
                .validDays(7)
                .build();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.of(issuedCoupon));

        // when
        int discountAmount = issuedCouponService.useCouponForOrder(userId, issuedCouponId, originalPrice);

        // then
        assertThat(discountAmount).isEqualTo(5000); // 50,000원의 10%
        assertThat(issuedCoupon.getStatus()).isEqualTo(CouponStatus.USED); // 상태가 USED로 변경되었는지 확인
        assertThat(issuedCoupon.getUsedAt()).isNotNull(); // 사용 시간이 기록되었는지 확인
        }
        }
