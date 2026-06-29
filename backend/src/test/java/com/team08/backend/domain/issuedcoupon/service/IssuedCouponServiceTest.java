package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponDownloadResponse;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategy;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategyFactory;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuedCouponServiceTest {

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssuedCouponStrategyFactory strategyFactory;

    @Mock
    private IssuedCouponWriter issuedCouponWriter;

    @Mock
    private IssuedCouponJobWriter issuedCouponJobWriter;

    @Mock
    private IssuedCouponJobStreamPublisher issuedCouponJobStreamPublisher;

    @Mock
    private FcfsCouponRedisIssuer fcfsCouponRedisIssuer;

    @Mock
    private AllUsersCouponMaterializer allUsersCouponMaterializer;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneId.systemDefault());

    private IssuedCouponService issuedCouponService;

    @BeforeEach
    void setUp() {
        issuedCouponService = new IssuedCouponService(
                issuedCouponRepository,
                couponPolicyRepository,
                userRepository,
                strategyFactory,
                issuedCouponWriter,
                issuedCouponJobWriter,
                issuedCouponJobStreamPublisher,
                allUsersCouponMaterializer,
                transactionTemplate,
                clock,
                fcfsCouponRedisIssuer
        );
    }

    @Test
    @DisplayName("성공: 쿠폰 다운로드 요청 시 팩토리를 통해 전략을 가져와 실행한다")
    void downloadCoupon_success() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        IssuedCouponStrategy strategy = mock(IssuedCouponStrategy.class);
        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findCouponTypeById(policyId)).thenReturn(Optional.of(CouponType.NORMAL));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(strategyFactory.getStrategy(CouponType.NORMAL)).thenReturn(strategy);
        when(strategy.issue(userId, policyId)).thenReturn(issuedCoupon);
        when(issuedCouponWriter.saveWithConcurrencyProtection(any(IssuedCoupon.class))).thenReturn(issuedCoupon);

        // when
        CouponDownloadResponse response = issuedCouponService.downloadCoupon(userId, policyId);

        // then
        assertThat(response).isNotNull();
        verify(transactionTemplate, times(1)).execute(any());
        verify(strategyFactory, times(1)).getStrategy(CouponType.NORMAL);
        verify(strategy, times(1)).issue(userId, policyId);
        verify(issuedCouponWriter, times(1)).saveWithConcurrencyProtection(any());
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 Stream 적재 실패 시 예외를 전파한다")
    void downloadFcfsCoupon_streamPublishFail_throwException() {
        // given
        Long userId = 1L;
        Long policyId = 1L;
        IssuedCouponStrategy strategy = mock(IssuedCouponStrategy.class);
        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        IssuedCouponJob issuedCouponJob = mock(IssuedCouponJob.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findCouponTypeById(policyId)).thenReturn(Optional.of(CouponType.FCFS));
        when(strategyFactory.getStrategy(CouponType.FCFS)).thenReturn(strategy);
        when(strategy.issue(userId, policyId)).thenReturn(issuedCoupon);
        when(issuedCouponJobStreamPublisher.publish(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(policyId)))
                .thenThrow(new IllegalStateException("stream"));

        // when & then
        assertThatThrownBy(() -> issuedCouponService.downloadCoupon(userId, policyId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("성공: 내 쿠폰 목록을 조회하면 정책 정보와 함께 반환된다")
    void getMyCoupons_success() {
        // given
        Long userId = 1L;
        Long policyId = 10L;

        IssuedCoupon coupon = mock(IssuedCoupon.class);
        when(coupon.getPolicyId()).thenReturn(policyId);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getId()).thenReturn(policyId);
        when(policy.getName()).thenReturn("테스트 쿠폰");

        when(issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(userId)).thenReturn(List.of(coupon));
        when(couponPolicyRepository.findAllById(anyList())).thenReturn(List.of(policy));

        // when
        List<CouponListResponse> responses = issuedCouponService.getMyCoupons(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).couponName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("전체 회원 쿠폰 발급 직후 내 쿠폰 조회 시 JIT 생성 후 목록을 반환한다")
    void getMyCoupons_afterAllUsersIssue_materializesAndReturnsCoupons() {
        // given
        Long userId = 1L;
        Long policyId = 10L;

        IssuedCoupon materializedCoupon = mock(IssuedCoupon.class);
        when(materializedCoupon.getPolicyId()).thenReturn(policyId);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getId()).thenReturn(policyId);
        when(policy.getName()).thenReturn("전체 회원 쿠폰");

        when(issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(userId)).thenReturn(List.of(materializedCoupon));
        when(couponPolicyRepository.findAllById(anyList())).thenReturn(List.of(policy));

        // when
        List<CouponListResponse> responses = issuedCouponService.getMyCoupons(userId);

        // then
        verify(allUsersCouponMaterializer).materializeForUser(userId);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).couponName()).isEqualTo("전체 회원 쿠폰");
    }

    @Test
    @DisplayName("성공: 쿠폰 적용 시 예상 할인 금액을 정확히 계산한다")
    void calculateExpectedDiscount_success() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 10000;
        LocalDateTime now = LocalDateTime.now(clock);

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        when(issuedCoupon.getPolicyId()).thenReturn(1L);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getName()).thenReturn("10% 할인 쿠폰");
        when(policy.calculateDiscountAmount(originalPrice)).thenReturn(1000);

        when(issuedCouponRepository.findById(issuedCouponId)).thenReturn(Optional.of(issuedCoupon));
        when(couponPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        ExpectedDiscountResponse response = issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, originalPrice);

        // then
        assertThat(response.discountAmount()).isEqualTo(1000);
        assertThat(response.finalPrice()).isEqualTo(9000);
        verify(issuedCoupon).validateUsable(userId, now);
    }

    @Test
    @DisplayName("전체 회원 쿠폰 발급 직후 쿠폰 적용 시 JIT 생성 후 할인 금액을 계산한다")
    void calculateExpectedDiscount_afterAllUsersIssue_materializesBeforeDiscountCalculation() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 30_000;
        LocalDateTime now = LocalDateTime.now(clock);

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        when(issuedCoupon.getPolicyId()).thenReturn(10L);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getName()).thenReturn("전체 회원 쿠폰");
        when(policy.calculateDiscountAmount(originalPrice)).thenReturn(5_000);

        when(issuedCouponRepository.findById(issuedCouponId)).thenReturn(Optional.of(issuedCoupon));
        when(couponPolicyRepository.findById(10L)).thenReturn(Optional.of(policy));

        // when
        ExpectedDiscountResponse response = issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, originalPrice);

        // then
        verify(allUsersCouponMaterializer).materializeForUser(userId);
        verify(issuedCoupon).validateUsable(userId, now);
        assertThat(response.couponName()).isEqualTo("전체 회원 쿠폰");
        assertThat(response.discountAmount()).isEqualTo(5_000);
        assertThat(response.finalPrice()).isEqualTo(25_000);
    }

    @Test
    @DisplayName("성공: 단회성 쿠폰 사용 시 상태가 USED로 변경된다")
    void useCouponForOrder_singleUse_success() {
        // given
        Long userId = 1L;
        Long issuedCouponId = 100L;
        int originalPrice = 10000;
        LocalDateTime now = LocalDateTime.now(clock);

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        when(issuedCoupon.getPolicyId()).thenReturn(1L);

        CouponPolicy policy = mock(CouponPolicy.class);
        when(policy.getUsageType()).thenReturn(CouponUsageType.SINGLE_USE);
        when(policy.calculateDiscountAmount(originalPrice)).thenReturn(1000);

        when(issuedCouponRepository.findByIdWithLock(issuedCouponId)).thenReturn(Optional.of(issuedCoupon));
        when(couponPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        int discountAmount = issuedCouponService.useCouponForOrder(userId, issuedCouponId, originalPrice);

        // then
        assertThat(discountAmount).isEqualTo(1000);
        verify(issuedCoupon).validateUsable(userId, now);
        verify(issuedCoupon).applyUsage(CouponUsageType.SINGLE_USE, now);
    }

}
