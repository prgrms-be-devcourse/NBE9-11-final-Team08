package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.service.CouponIssueExecutor;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CouponIssueRequestProcessorTest {

    @Mock
    private CouponIssueRequestRepository couponIssueRequestRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponIssueExecutor couponIssueExecutor;

    private CouponIssueRequestProcessor couponIssueRequestProcessor;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        couponIssueRequestProcessor = new CouponIssueRequestProcessor(
                couponIssueRequestRepository,
                couponPolicyRepository,
                userRepository,
                couponIssueExecutor,
                clock
        );
    }

    @Test
    @DisplayName("회원별 메시지를 처리해 쿠폰을 발급하고 성공 카운트를 증가시킨다")
    void processSelectedUser_issuesCouponAndIncreasesSuccessCount() {
        // given
        CouponPolicy policy = manualIssuePolicy();
        given(userRepository.existsById(1L)).willReturn(true);
        given(couponPolicyRepository.findById(10L)).willReturn(Optional.of(policy));
        given(couponIssueExecutor.issueRewardCoupon(1L, policy, "SELECTED_USERS_VIP_EVENT_2026", "SELECTED_USERS"))
                .willReturn(CouponIssueExecutor.CouponIssueResult.issued(1000L));

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        then(couponIssueRequestRepository).should().incrementSuccessCount(100L);
        then(couponIssueRequestRepository).should(never()).incrementSkippedCount(100L);
        then(couponIssueRequestRepository).should(never()).incrementFailedCount(100L);
        then(couponIssueRequestRepository).should().completeIfProcessed(100L, java.time.LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("이미 발급된 쿠폰이면 스킵 카운트를 증가시킨다")
    void processSelectedUser_increasesSkippedCountWhenAlreadyProcessed() {
        // given
        CouponPolicy policy = manualIssuePolicy();
        given(userRepository.existsById(1L)).willReturn(true);
        given(couponPolicyRepository.findById(10L)).willReturn(Optional.of(policy));
        given(couponIssueExecutor.issueRewardCoupon(1L, policy, "SELECTED_USERS_VIP_EVENT_2026", "SELECTED_USERS"))
                .willReturn(CouponIssueExecutor.CouponIssueResult.skipped());

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        then(couponIssueRequestRepository).should().incrementSkippedCount(100L);
        then(couponIssueRequestRepository).should(never()).incrementSuccessCount(100L);
        then(couponIssueRequestRepository).should(never()).incrementFailedCount(100L);
        then(couponIssueRequestRepository).should().completeIfProcessed(100L, java.time.LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("정책이 관리자 발급용이 아니면 실패 카운트를 증가시킨다")
    void processSelectedUser_increasesFailedCountWhenPolicyInvalid() {
        // given
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponType()).willReturn(CouponType.AUTO);
        given(policy.getAutoIssueType()).willReturn(AutoIssueType.SIGNUP);
        given(userRepository.existsById(1L)).willReturn(true);
        given(couponPolicyRepository.findById(10L)).willReturn(Optional.of(policy));

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        then(couponIssueRequestRepository).should().incrementFailedCount(100L);
        then(couponIssueRequestRepository).should(never()).incrementSuccessCount(100L);
        then(couponIssueRequestRepository).should(never()).incrementSkippedCount(100L);
        then(couponIssueRequestRepository).should().completeIfProcessed(100L, java.time.LocalDateTime.now(clock));
        then(couponIssueExecutor).should(never()).issueRewardCoupon(any(), any(), any(), any());
    }

    @Test
    @DisplayName("회원이 없으면 실패 카운트를 증가시킨다")
    void processSelectedUser_increasesFailedCountWhenUserNotFound() {
        // given
        given(userRepository.existsById(1L)).willReturn(false);

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        then(couponIssueRequestRepository).should().incrementFailedCount(100L);
        then(couponPolicyRepository).shouldHaveNoInteractions();
        then(couponIssueExecutor).shouldHaveNoInteractions();
        then(couponIssueRequestRepository).should().completeIfProcessed(100L, java.time.LocalDateTime.now(clock));
    }

    private CouponPolicy manualIssuePolicy() {
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponType()).willReturn(CouponType.AUTO);
        given(policy.getAutoIssueType()).willReturn(null);
        return policy;
    }
}
