package com.team08.backend.domain.issuedcoupon.scheduler;

import com.team08.backend.domain.couponpolicy.entity.*;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
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
        User user = saveUser("scheduler_test@test.com");
        CouponPolicy policy1 = savePolicy("테스트정책1");
        CouponPolicy policy2 = savePolicy("테스트정책2");

        // 만료일이 지난 쿠폰 생성
        IssuedCoupon expiredCoupon = IssuedCoupon.create(policy1, user.getId());
        ReflectionTestUtils.setField(expiredCoupon, "expiredAt", LocalDateTime.now().minusMinutes(1));
        issuedCouponRepository.save(expiredCoupon);

        // 유효기간이 남은 쿠폰 생성
        IssuedCoupon validCoupon = IssuedCoupon.create(policy2, user.getId());
        ReflectionTestUtils.setField(validCoupon, "expiredAt", LocalDateTime.now().plusDays(1));
        issuedCouponRepository.save(validCoupon);

        // when
        couponScheduler.expireCoupons();

        // then
        IssuedCoupon updatedExpiredCoupon = issuedCouponRepository.findById(expiredCoupon.getId()).get();
        IssuedCoupon updatedValidCoupon = issuedCouponRepository.findById(validCoupon.getId()).get();

        assertThat(updatedExpiredCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        assertThat(updatedValidCoupon.getStatus()).isEqualTo(CouponStatus.ISSUED);
    }

    private User saveUser(String email) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "테스터");
        ReflectionTestUtils.setField(user, "role", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return userRepository.save(user);
    }

    private CouponPolicy savePolicy(String name) {
        try {
            var constructor = CouponPolicy.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CouponPolicy policy = constructor.newInstance();
            ReflectionTestUtils.setField(policy, "name", name);
            ReflectionTestUtils.setField(policy, "discountType", DiscountType.AMOUNT);
            ReflectionTestUtils.setField(policy, "discountValue", 1000);
            ReflectionTestUtils.setField(policy, "validDays", 7);
            ReflectionTestUtils.setField(policy, "totalQuantity", 100);
            ReflectionTestUtils.setField(policy, "couponType", CouponType.NORMAL);
            ReflectionTestUtils.setField(policy, "couponTarget", CouponTarget.ALL);
            ReflectionTestUtils.setField(policy, "usageType", CouponUsageType.SINGLE_USE);
            ReflectionTestUtils.setField(policy, "isStackable", false);
            return couponPolicyRepository.save(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test entity.", e);
        }
    }
}
