package com.team08.backend.domain.couponreward.service;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.couponreward.entity.CouponRewardHistory;
import com.team08.backend.domain.couponreward.repository.CouponRewardHistoryRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.service.CouponIssueExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CouponRewardServiceTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private CouponRewardHistoryRepository couponRewardHistoryRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    private CouponRewardService couponRewardService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        CouponIssueExecutor couponIssueExecutor = new CouponIssueExecutor(
                couponRewardHistoryRepository,
                issuedCouponRepository,
                clock
        );
        couponRewardService = new CouponRewardService(
                couponPolicyRepository,
                couponIssueExecutor,
                clock
        );
    }

    @Test
    @DisplayName("회원가입 보상은 SIGNUP 활성 정책으로 쿠폰을 발급한다")
    void issueSignupReward_issuesCouponWithActiveSignupPolicy() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(issuedCouponRepository.findByUserIdAndPolicyId(userId, 10L))
                .willReturn(Optional.empty());
        given(couponRewardHistoryRepository.save(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.save(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 100L);
                    return coupon;
                });

        // when
        couponRewardService.issueSignupReward(userId);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(historyCaptor.getValue().getPolicyId()).isEqualTo(10L);
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("SIGNUP");
        assertThat(historyCaptor.getValue().getRewardType()).isEqualTo("SIGNUP");

        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().save(couponCaptor.capture());
        assertThat(couponCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(couponCaptor.getValue().getPolicyId()).isEqualTo(10L);
        assertThat(couponCaptor.getValue().getIssueKey()).isEqualTo("SIGNUP");
    }

    @Test
    @DisplayName("회원가입 후 보상 처리 시 회원가입 쿠폰이 발급됐는지 확인한다")
    void issueSignupReward_afterSignupCouponRewardIssuesCoupon() {
        // given
        Long signedUpUserId = 1L;
        CouponPolicy signupPolicy = policy(10L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(signupPolicy));
        given(issuedCouponRepository.findByUserIdAndPolicyId(signedUpUserId, 10L))
                .willReturn(Optional.empty());
        given(couponRewardHistoryRepository.save(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.save(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 100L);
                    return coupon;
                });

        // when
        couponRewardService.issueSignupReward(signedUpUserId);

        // then
        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().save(couponCaptor.capture());
        IssuedCoupon issuedCoupon = couponCaptor.getValue();
        assertThat(issuedCoupon.getUserId()).isEqualTo(signedUpUserId);
        assertThat(issuedCoupon.getPolicyId()).isEqualTo(10L);
        assertThat(issuedCoupon.getIssueKey()).isEqualTo("SIGNUP");
        assertThat(issuedCoupon.getIssuedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("연속 출석일이 7의 배수이면 회차별 보상키로 쿠폰을 발급한다")
    void issueAttendanceStreakReward_issuesCouponWhenConsecutiveDaysIsMultipleOfSeven() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.ATTENDANCE_STREAK), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(issuedCouponRepository.findByUserIdAndPolicyId(userId, 10L))
                .willReturn(Optional.empty());
        given(couponRewardHistoryRepository.save(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.save(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 100L);
                    return coupon;
                });

        // when
        couponRewardService.issueAttendanceStreakReward(userId, 14);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("ATTENDANCE_STREAK_14");

        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().save(couponCaptor.capture());
        assertThat(couponCaptor.getValue().getIssueKey()).isEqualTo("ATTENDANCE_STREAK_14");
    }

    @Test
    @DisplayName("이번달 누적 출석이 15일이면 월간 보상키로 쿠폰을 발급한다")
    void issueMonthlyAttendanceReward_issuesCouponWhenMonthlyTotalDaysIsFifteen() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(20L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.MONTHLY_ATTENDANCE), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(issuedCouponRepository.findByUserIdAndPolicyId(userId, 20L))
                .willReturn(Optional.empty());
        given(couponRewardHistoryRepository.save(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.save(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 200L);
                    return coupon;
                });

        // when
        couponRewardService.issueMonthlyAttendanceReward(userId, "2026-06", 15);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("MONTHLY_ATTENDANCE_15_2026-06");
        assertThat(historyCaptor.getValue().getRewardType()).isEqualTo("MONTHLY_ATTENDANCE");

        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().save(couponCaptor.capture());
        assertThat(couponCaptor.getValue().getIssueKey()).isEqualTo("MONTHLY_ATTENDANCE_15_2026-06");
    }

    @Test
    @DisplayName("보상 이력이 이미 있으면 쿠폰을 중복 발급하지 않는다")
    void issueReward_doesNotIssueDuplicateCouponWhenRewardHistoryExists() {
        // given
        Long userId = 1L;
        CouponPolicy policy = org.mockito.Mockito.mock(CouponPolicy.class);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(couponRewardHistoryRepository.existsByUserIdAndRewardKey(userId, "SIGNUP"))
                .willReturn(true);

        // when
        couponRewardService.issueSignupReward(userId);

        // then
        then(couponRewardHistoryRepository).should(never()).save(any(CouponRewardHistory.class));
        then(issuedCouponRepository).should(never()).save(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("발급된 쿠폰은 있고 보상 이력이 없으면 이력을 보정한다")
    void issueReward_repairsRewardHistoryWhenCouponAlreadyIssued() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        IssuedCoupon issuedCoupon = IssuedCoupon.create(
                policy,
                userId,
                "SIGNUP",
                LocalDateTime.of(2026, 6, 15, 9, 0)
        );
        ReflectionTestUtils.setField(issuedCoupon, "id", 100L);

        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(issuedCouponRepository.findByUserIdAndPolicyId(userId, 10L))
                .willReturn(Optional.of(issuedCoupon));

        // when
        couponRewardService.issueSignupReward(userId);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getIssuedCouponId()).isEqualTo(100L);
        then(issuedCouponRepository).should(never()).save(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("보상키가 달라도 같은 정책 쿠폰을 이미 받았으면 중복 발급하지 않는다")
    void issueReward_doesNotIssueDuplicateCouponWhenSamePolicyAlreadyIssuedWithDifferentRewardKey() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        IssuedCoupon issuedCoupon = IssuedCoupon.create(
                policy,
                userId,
                "SIGNUP",
                LocalDateTime.of(2026, 6, 15, 9, 0)
        );
        ReflectionTestUtils.setField(issuedCoupon, "id", 100L);

        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.ATTENDANCE_STREAK), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(issuedCouponRepository.findByUserIdAndPolicyId(userId, 10L))
                .willReturn(Optional.of(issuedCoupon));

        // when
        couponRewardService.issueAttendanceStreakReward(userId, 14);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("ATTENDANCE_STREAK_14");
        assertThat(historyCaptor.getValue().getIssuedCouponId()).isEqualTo(100L);
        then(issuedCouponRepository).should(never()).save(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("활성 자동발급 정책이 없으면 발급하지 않는다")
    void issueSignupReward_doesNotIssueWhenActivePolicyDoesNotExist() {
        // given
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.empty());

        // when
        couponRewardService.issueSignupReward(1L);

        // then
        then(couponRewardHistoryRepository).shouldHaveNoInteractions();
        then(issuedCouponRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("연속 출석일이 7의 배수가 아니면 발급하지 않는다")
    void issueAttendanceStreakReward_doesNotIssueWhenConsecutiveDaysIsNotMultipleOfSeven() {
        // when
        couponRewardService.issueAttendanceStreakReward(1L, 13);

        // then
        then(couponPolicyRepository).shouldHaveNoInteractions();
        then(couponRewardHistoryRepository).shouldHaveNoInteractions();
        then(issuedCouponRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이번달 누적 출석이 15일이 아니면 발급하지 않는다")
    void issueMonthlyAttendanceReward_doesNotIssueWhenMonthlyTotalDaysIsNotFifteen() {
        // when
        couponRewardService.issueMonthlyAttendanceReward(1L, "2026-06", 14);

        // then
        then(couponPolicyRepository).shouldHaveNoInteractions();
        then(couponRewardHistoryRepository).shouldHaveNoInteractions();
        then(issuedCouponRepository).shouldHaveNoInteractions();
    }

    private CouponPolicy policy(Long policyId) {
        CouponPolicy policy = Mockito.mock(CouponPolicy.class);
        given(policy.getId()).willReturn(policyId);
        given(policy.calculateExpirationDate(any(LocalDateTime.class)))
                .willAnswer(invocation -> invocation.<LocalDateTime>getArgument(0).plusDays(30));
        return policy;
    }
}
