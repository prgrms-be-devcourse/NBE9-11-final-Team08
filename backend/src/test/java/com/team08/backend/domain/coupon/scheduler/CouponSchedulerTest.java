package com.team08.backend.domain.coupon.scheduler;

import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.entity.CouponTarget;
import com.team08.backend.domain.coupon.entity.CouponType;
import com.team08.backend.domain.coupon.entity.DiscountType;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import com.team08.backend.domain.coupon.repository.CouponPolicyRepository;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CouponSchedulerTest {

    @Autowired
    private CouponScheduler couponScheduler;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Test
    @DisplayName("스케줄러 실행 시 만료일이 지난 쿠폰의 상태가 EXPIRED로 변경된다")
    void expireCoupons_RealStateChange() {
        // given
        User user = userRepository.save(User.create("test@test.com", "password", "테스트유저"));
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.builder()
                .name("테스트정책")
                .validDays(7)
                .couponTarget(CouponTarget.ALL)
                .couponType(CouponType.NORMAL)
                .discountType(DiscountType.AMOUNT)
                .discountValue(1000)
                .totalQuantity(100)
                .build());

        // 만료일이 지난 쿠폰
        IssuedCoupon expiredCoupon = issuedCouponRepository.save(
                IssuedCoupon.builder()
                        .user(user)
                        .policy(policy)
                        .expiredAt(LocalDateTime.now().minusDays(1))
                        .build()
        );

        // 유효기간이 남은 쿠폰
        IssuedCoupon validCoupon = issuedCouponRepository.save(
                IssuedCoupon.builder()
                        .user(user)
                        .policy(policy)
                        .expiredAt(LocalDateTime.now().plusDays(1))
                        .build()
        );

        // when
        couponScheduler.expireCoupons();

        // then
        IssuedCoupon updatedExpiredCoupon = issuedCouponRepository.findById(expiredCoupon.getId()).get();
        IssuedCoupon updatedValidCoupon = issuedCouponRepository.findById(validCoupon.getId()).get();

        assertThat(updatedExpiredCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        assertThat(updatedValidCoupon.getStatus()).isEqualTo(CouponStatus.ISSUED);
    }
}
