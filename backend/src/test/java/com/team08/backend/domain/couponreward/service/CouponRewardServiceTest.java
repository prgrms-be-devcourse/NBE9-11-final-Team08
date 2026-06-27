package com.team08.backend.domain.couponreward.service;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.couponreward.entity.CouponRewardHistory;
import com.team08.backend.domain.couponreward.repository.CouponRewardHistoryRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.mockito.Mockito.lenient;
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
        couponRewardService = new CouponRewardService(
                couponPolicyRepository,
                couponRewardHistoryRepository,
                issuedCouponRepository,
                clock
        );
    }

    @Test
    void 회원가입_보상은_SIGNUP_활성_정책으로_쿠폰을_발급한다() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(couponRewardHistoryRepository.saveAndFlush(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 100L);
                    return coupon;
                });

        // when
        couponRewardService.issueSignupReward(userId);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(historyCaptor.getValue().getPolicyId()).isEqualTo(10L);
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("SIGNUP");
        assertThat(historyCaptor.getValue().getRewardType()).isEqualTo("SIGNUP");

        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().saveAndFlush(couponCaptor.capture());
        assertThat(couponCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(couponCaptor.getValue().getPolicyId()).isEqualTo(10L);
        assertThat(couponCaptor.getValue().getIssueKey()).isEqualTo("SIGNUP");
    }

    @Test
    void 연속_출석일이_7의_배수이면_회차별_보상키로_쿠폰을_발급한다() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.ATTENDANCE_STREAK), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(couponRewardHistoryRepository.saveAndFlush(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 100L);
                    return coupon;
                });

        // when
        couponRewardService.issueAttendanceStreakReward(userId, 14);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("ATTENDANCE_STREAK_14");

        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().saveAndFlush(couponCaptor.capture());
        assertThat(couponCaptor.getValue().getIssueKey()).isEqualTo("ATTENDANCE_STREAK_14");
    }

    @Test
    void 이번달_누적_출석이_15일이면_월간_보상키로_쿠폰을_발급한다() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(20L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.MONTHLY_ATTENDANCE), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(couponRewardHistoryRepository.saveAndFlush(any(CouponRewardHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class)))
                .willAnswer(invocation -> {
                    IssuedCoupon coupon = invocation.getArgument(0);
                    ReflectionTestUtils.setField(coupon, "id", 200L);
                    return coupon;
                });

        // when
        couponRewardService.issueMonthlyAttendanceReward(userId, "2026-06", 15);

        // then
        ArgumentCaptor<CouponRewardHistory> historyCaptor = ArgumentCaptor.forClass(CouponRewardHistory.class);
        then(couponRewardHistoryRepository).should().saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRewardKey()).isEqualTo("MONTHLY_ATTENDANCE_15_2026-06");
        assertThat(historyCaptor.getValue().getRewardType()).isEqualTo("MONTHLY_ATTENDANCE");

        ArgumentCaptor<IssuedCoupon> couponCaptor = ArgumentCaptor.forClass(IssuedCoupon.class);
        then(issuedCouponRepository).should().saveAndFlush(couponCaptor.capture());
        assertThat(couponCaptor.getValue().getIssueKey()).isEqualTo("MONTHLY_ATTENDANCE_15_2026-06");
    }

    @Test
    void 보상_이력이_이미_있으면_쿠폰을_중복_발급하지_않는다() {
        // given
        Long userId = 1L;
        CouponPolicy policy = policy(10L);
        given(couponPolicyRepository.findActiveByAutoIssueType(eq(AutoIssueType.SIGNUP), any(LocalDateTime.class)))
                .willReturn(Optional.of(policy));
        given(couponRewardHistoryRepository.saveAndFlush(any(CouponRewardHistory.class)))
                .willThrow(new DataIntegrityViolationException("duplicate reward"));

        // when
        couponRewardService.issueSignupReward(userId);

        // then
        then(issuedCouponRepository).should(never()).saveAndFlush(any(IssuedCoupon.class));
    }

    @Test
    void 활성_자동발급_정책이_없으면_발급하지_않는다() {
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
    void 연속_출석일이_7의_배수가_아니면_발급하지_않는다() {
        // when
        couponRewardService.issueAttendanceStreakReward(1L, 13);

        // then
        then(couponPolicyRepository).shouldHaveNoInteractions();
        then(couponRewardHistoryRepository).shouldHaveNoInteractions();
        then(issuedCouponRepository).shouldHaveNoInteractions();
    }

    @Test
    void 이번달_누적_출석이_15일이_아니면_발급하지_않는다() {
        // when
        couponRewardService.issueMonthlyAttendanceReward(1L, "2026-06", 14);

        // then
        then(couponPolicyRepository).shouldHaveNoInteractions();
        then(couponRewardHistoryRepository).shouldHaveNoInteractions();
        then(issuedCouponRepository).shouldHaveNoInteractions();
    }

    private CouponPolicy policy(Long policyId) {
        CouponPolicy policy = org.mockito.Mockito.mock(CouponPolicy.class);
        given(policy.getId()).willReturn(policyId);
        lenient().when(policy.calculateExpirationDate(any(LocalDateTime.class)))
                .thenAnswer(invocation -> invocation.<LocalDateTime>getArgument(0).plusDays(30));
        return policy;
    }
}
