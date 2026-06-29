package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponissuerequest.service.CouponIssueSuccessCountRedisCounter;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AllUsersCouponMaterializerTest {

    @Mock
    private CouponIssueRequestRepository couponIssueRequestRepository;

    @Mock
    private IssuedCouponWriter issuedCouponWriter;

    @Mock
    private CouponIssueSuccessCountRedisCounter successCountRedisCounter;

    private AllUsersCouponMaterializer materializer;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        materializer = new AllUsersCouponMaterializer(
                couponIssueRequestRepository,
                successCountRedisCounter,
                issuedCouponWriter,
                clock
        );
    }

    @Test
    @DisplayName("전체 회원 grant 대상 쿠폰을 사용자 접근 시점에 한 번만 생성한다")
    void materializeForUser_createsIssuedCouponOncePerPolicy() {
        // given
        CouponPolicy policy = manualAllUsersPolicy(10L);
        LocalDateTime now = LocalDateTime.now(clock);
        given(couponIssueRequestRepository.findMaterializableAllUsersPolicies(1L, now))
                .willReturn(List.of(policy, policy));
        given(issuedCouponWriter.saveAllWithConcurrencyProtection(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        materializer.materializeForUser(1L);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<IssuedCoupon>> listCaptor = ArgumentCaptor.forClass(List.class);
        then(issuedCouponWriter).should(times(1)).saveAllWithConcurrencyProtection(listCaptor.capture());
        List<IssuedCoupon> coupons = listCaptor.getValue();
        assertThat(coupons).hasSize(1);
        IssuedCoupon coupon = coupons.get(0);
        assertThat(coupon.getUserId()).isEqualTo(1L);
        assertThat(coupon.getPolicyId()).isEqualTo(10L);
        assertThat(coupon.getIssueKey()).isEqualTo("ALL_USERS");
        assertThat(coupon.getIssuedAt()).isEqualTo(now);
        then(successCountRedisCounter).should(times(1)).incrementSuccessCount(10L);
    }

    @Test
    @DisplayName("동시 요청으로 이미 생성된 쿠폰이면 중복 예외를 정상 경합으로 무시한다")
    void materializeForUser_ignoresConcurrentDuplicate() {
        // given
        CouponPolicy policy = manualAllUsersPolicy(10L);
        LocalDateTime now = LocalDateTime.now(clock);
        given(couponIssueRequestRepository.findMaterializableAllUsersPolicies(1L, now))
                .willReturn(List.of(policy));
        given(issuedCouponWriter.saveAllWithConcurrencyProtection(any()))
                .willThrow(new CouponAlreadyIssuedException());
        given(issuedCouponWriter.saveWithConcurrencyProtection(any(IssuedCoupon.class)))
                .willThrow(new CouponAlreadyIssuedException());

        // when
        materializer.materializeForUser(1L);

        // then
        then(issuedCouponWriter).should().saveAllWithConcurrencyProtection(any());
        then(issuedCouponWriter).should().saveWithConcurrencyProtection(any(IssuedCoupon.class));
    }

    private CouponPolicy manualAllUsersPolicy(Long policyId) {
        CouponPolicy policy = CouponPolicy.createPolicy(
                "전체 회원 쿠폰",
                CouponTarget.ALL,
                CouponType.ADMIN,
                null,
                CouponUsageType.SINGLE_USE,
                false,
                DiscountType.AMOUNT,
                1000,
                null,
                null,
                7,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59),
                List.of(),
                List.of()
        );
        ReflectionTestUtils.setField(policy, "id", policyId);
        return policy;
    }
}
