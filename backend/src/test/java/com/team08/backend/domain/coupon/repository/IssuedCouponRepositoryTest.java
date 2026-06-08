package com.team08.backend.domain.coupon.repository;

import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.entity.CouponTarget;
import com.team08.backend.domain.coupon.entity.CouponType;
import com.team08.backend.domain.coupon.entity.DiscountType;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IssuedCouponRepositoryTest {

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("만료 시간이 지난 쿠폰은 EXPIRED 상태로 업데이트되어야 한다")
    void expirePastCoupons_test() {
        // 1. Given: 테스트 데이터 준비
        User user = User.builder().nickname("테스터").email("test@test.com").build();
        userRepository.save(user);

        CouponPolicy policy = CouponPolicy.builder()
                .name("만료 테스트 쿠폰")
                .discountType(DiscountType.AMOUNT)
                .discountValue(1000)
                .validDays(1)
                .couponType(CouponType.NORMAL)
                .couponTarget(CouponTarget.ALL)
                .build();
        couponPolicyRepository.save(policy);

        // 어제 발급되어 오늘 만료된 쿠폰 (만료 시간: 어제)
        IssuedCoupon coupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(LocalDateTime.now().minusDays(1))
                .build();
        issuedCouponRepository.save(coupon);

        // 2. When: 만료 처리 로직 실행
        int updatedCount = issuedCouponRepository.expirePastCoupons(
                LocalDateTime.now(),
                CouponStatus.ISSUED,
                CouponStatus.EXPIRED
        );

        // 3. Then: 검증
        assertThat(updatedCount).isEqualTo(1); // 1개가 만료 처리되었는지

        IssuedCoupon updatedCoupon = issuedCouponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED); // 상태가 EXPIRED로 바뀌었는지
    }
}
