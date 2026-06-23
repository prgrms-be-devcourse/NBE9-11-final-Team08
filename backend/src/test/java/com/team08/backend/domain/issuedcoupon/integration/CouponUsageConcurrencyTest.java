package com.team08.backend.domain.issuedcoupon.integration;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponUsageConcurrencyTest {

    @Autowired
    private IssuedCouponService issuedCouponService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("동시성 테스트: 동일한 쿠폰을 여러 스레드가 동시에 사용하려고 할 때 비관적 락으로 1건만 성공해야 한다")
    void useCouponForOrder_Concurrency_Test() throws InterruptedException {
        // given
        User user = saveUser("concurrency_test@example.com");
        CouponPolicy policy = savePolicy("동시성 테스트 쿠폰", CouponType.NORMAL);
        IssuedCoupon issuedCoupon = saveIssuedCoupon(user.getId(), policy);

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 동일한 사용자가 동일한 쿠폰을 결제에 사용 시도
                    issuedCouponService.useCouponForOrder(user.getId(), issuedCoupon.getId(), 10000);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 비관적 락 덕분에 한 트랜잭션만 성공하고, 나머지는 상태가 USED로 변경된 쿠폰을 조회하게 되어 실패해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // 쿠폰 상태는 USED여야 함
        IssuedCoupon updatedCoupon = issuedCouponRepository.findById(issuedCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
    }

    private User saveUser(String email) {
        User user = createUserInstance();
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "동시성테스터");
        ReflectionTestUtils.setField(user, "role", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return userRepository.save(user);
    }

    private CouponPolicy savePolicy(String name, CouponType type) {
        return couponPolicyRepository.save(CouponPolicy.createPolicy(
                name, CouponTarget.ALL, type, null,
                CouponUsageType.SINGLE_USE, false, DiscountType.AMOUNT, 1000, null, null, 7, null, null, null, null
        ));
    }

    private IssuedCoupon saveIssuedCoupon(Long userId, CouponPolicy policy) {
        LocalDateTime now = LocalDateTime.now();
        IssuedCoupon coupon = IssuedCoupon.create(policy, userId, now);
        return issuedCouponRepository.save(coupon);
    }

    private User createUserInstance() {
        try {
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test User entity.", e);
        }
    }
}
