package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponDownloadResponse;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategy;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategyFactory;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import com.team08.backend.domain.issuedcouponjob.repository.IssuedCouponJobRepository;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobProcessor;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobStreamPublisher;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobWriter;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
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
    private IssuedCouponJobRepository issuedCouponJobRepository;

    @Mock
    private IssuedCouponJobProcessor issuedCouponJobProcessor;

    @Mock
    private IssuedCouponJobWriter issuedCouponJobWriter;

    @Mock
    private IssuedCouponJobStreamPublisher issuedCouponJobStreamPublisher;

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
                issuedCouponJobRepository,
                issuedCouponJobProcessor,
                issuedCouponJobWriter,
                issuedCouponJobStreamPublisher,
                clock
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
        IssuedCouponJob issuedCouponJob = mock(IssuedCouponJob.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponPolicyRepository.findCouponTypeById(policyId)).thenReturn(Optional.of(CouponType.NORMAL));
        when(strategyFactory.getStrategy(CouponType.NORMAL)).thenReturn(strategy);
        when(strategy.issue(userId, policyId)).thenReturn(issuedCoupon);
        when(issuedCouponJobWriter.createRequested(any(), any(), any())).thenReturn(issuedCouponJob);
        when(issuedCouponJob.getId()).thenReturn(1L);

        // writer 모킹
        when(issuedCouponWriter.saveWithConcurrencyProtection(any(IssuedCoupon.class))).thenReturn(issuedCoupon);

        // when
        CouponDownloadResponse response = issuedCouponService.downloadCoupon(userId, policyId);

        // then
        assertThat(response).isNotNull();
        verify(strategyFactory, times(1)).getStrategy(CouponType.NORMAL);
        verify(strategy, times(1)).issue(userId, policyId);
        verify(issuedCouponWriter, times(1)).saveWithConcurrencyProtection(any());
        verify(issuedCouponJobWriter).markIssued(any(), any());
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 Stream 적재 실패 시 재시도 상태로 남기고 요청 응답을 반환한다")
    void downloadFcfsCoupon_streamPublishFail_retrying() {
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
        when(issuedCouponJobWriter.createRequested(any(), any(), any())).thenReturn(issuedCouponJob);
        when(issuedCouponJob.getId()).thenReturn(1L);
        when(issuedCouponJobStreamPublisher.publish(1L, userId, policyId))
                .thenThrow(new DataIntegrityViolationException("stream"));

        // when
        CouponDownloadResponse response = issuedCouponService.downloadCoupon(userId, policyId);

        // then
        assertThat(response).isNotNull();
        verify(issuedCouponJobWriter).markRetrying(any(), any(), any());
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

        when(issuedCouponRepository.findById(issuedCouponId)).thenReturn(Optional.of(issuedCoupon));
        when(couponPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        int discountAmount = issuedCouponService.useCouponForOrder(userId, issuedCouponId, originalPrice);

        // then
        assertThat(discountAmount).isEqualTo(1000);
        verify(issuedCoupon).validateUsable(userId, now);
        verify(issuedCoupon).applyUsage(CouponUsageType.SINGLE_USE, now);
    }

    @Test
    @DisplayName("성공: 발급 작업 즉시 반영 시 처리 가능한 Job이면 DB 저장을 시도하고 발급 완료 응답을 반환한다")
    void completeCouponIssueJob_processable_success() {
        // given
        Long userId = 1L;
        Long jobId = 10L;
        Long policyId = 100L;
        IssuedCouponJob job = mock(IssuedCouponJob.class);
        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        when(issuedCouponJobRepository.findByIdAndUserId(jobId, userId))
                .thenReturn(Optional.of(job))
                .thenReturn(Optional.of(job));
        when(job.isProcessable()).thenReturn(true);
        when(job.getPolicyId()).thenReturn(policyId);
        when(job.getId()).thenReturn(jobId);
        when(issuedCouponRepository.findByUserIdAndPolicyId(userId, policyId)).thenReturn(Optional.of(issuedCoupon));
        when(issuedCoupon.getId()).thenReturn(1L);
        when(issuedCoupon.getPolicyId()).thenReturn(policyId);
        when(issuedCoupon.getUserId()).thenReturn(userId);
        when(issuedCoupon.getStatus()).thenReturn(com.team08.backend.domain.issuedcoupon.entity.CouponStatus.ISSUED);
        when(issuedCoupon.getIssuedAt()).thenReturn(LocalDateTime.now(clock));
        when(issuedCoupon.getExpiredAt()).thenReturn(LocalDateTime.now(clock).plusDays(7));

        // when
        CouponDownloadResponse response = issuedCouponService.completeCouponIssueJob(userId, jobId);

        // then
        assertThat(response.jobStatus()).isEqualTo(IssuedCouponJobStatus.ISSUED);
        assertThat(response.issuedCouponId()).isEqualTo(1L);
        verify(issuedCouponJobProcessor).process(jobId);
    }
}
