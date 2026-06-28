package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestStatus;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        CouponIssueRequest request = request(1);
        CouponPolicy policy = manualIssuePolicy();
        given(couponIssueRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(request));
        given(userRepository.existsById(1L)).willReturn(true);
        given(couponPolicyRepository.findById(10L)).willReturn(Optional.of(policy));
        given(couponIssueExecutor.issueRewardCoupon(1L, policy, "SELECTED_USERS_VIP_EVENT_2026", "SELECTED_USERS"))
                .willReturn(CouponIssueExecutor.CouponIssueResult.issued(1000L));

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        assertThat(request.getSuccessCount()).isEqualTo(1);
        assertThat(request.getFailedCount()).isZero();
        assertThat(request.getSkippedCount()).isZero();
        assertThat(request.getStatus()).isEqualTo(CouponIssueRequestStatus.COMPLETED);
    }

    @Test
    @DisplayName("이미 발급된 쿠폰이면 스킵 카운트를 증가시킨다")
    void processSelectedUser_increasesSkippedCountWhenAlreadyProcessed() {
        // given
        CouponIssueRequest request = request(1);
        CouponPolicy policy = manualIssuePolicy();
        given(couponIssueRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(request));
        given(userRepository.existsById(1L)).willReturn(true);
        given(couponPolicyRepository.findById(10L)).willReturn(Optional.of(policy));
        given(couponIssueExecutor.issueRewardCoupon(1L, policy, "SELECTED_USERS_VIP_EVENT_2026", "SELECTED_USERS"))
                .willReturn(CouponIssueExecutor.CouponIssueResult.skipped());

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        assertThat(request.getSuccessCount()).isZero();
        assertThat(request.getFailedCount()).isZero();
        assertThat(request.getSkippedCount()).isEqualTo(1);
        assertThat(request.getStatus()).isEqualTo(CouponIssueRequestStatus.COMPLETED);
    }

    @Test
    @DisplayName("정책이 관리자 발급용이 아니면 실패 카운트를 증가시킨다")
    void processSelectedUser_increasesFailedCountWhenPolicyInvalid() {
        // given
        CouponIssueRequest request = request(1);
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponType()).willReturn(CouponType.AUTO);
        given(policy.getAutoIssueType()).willReturn(AutoIssueType.SIGNUP);
        given(couponIssueRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(request));
        given(userRepository.existsById(1L)).willReturn(true);
        given(couponPolicyRepository.findById(10L)).willReturn(Optional.of(policy));

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        assertThat(request.getSuccessCount()).isZero();
        assertThat(request.getFailedCount()).isEqualTo(1);
        assertThat(request.getSkippedCount()).isZero();
        assertThat(request.getStatus()).isEqualTo(CouponIssueRequestStatus.COMPLETED);
        then(couponIssueExecutor).should(never()).issueRewardCoupon(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 종료된 요청이면 메시지를 처리하지 않는다")
    void processSelectedUser_skipsWhenRequestFinished() {
        // given
        CouponIssueRequest request = request(1);
        request.markCompleted(LocalDateTime.now(clock));
        given(couponIssueRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(request));

        // when
        couponIssueRequestProcessor.processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");

        // then
        then(userRepository).shouldHaveNoInteractions();
        then(couponPolicyRepository).shouldHaveNoInteractions();
        then(couponIssueExecutor).shouldHaveNoInteractions();
    }

    private CouponIssueRequest request(long requestedCount) {
        CouponIssueRequest request = CouponIssueRequest.request(
                10L,
                "VIP_EVENT_2026",
                CouponIssueRequestType.SELECTED_USERS,
                99L,
                LocalDateTime.now(clock)
        );
        request.addRequestedCount(requestedCount);
        return request;
    }

    private CouponPolicy manualIssuePolicy() {
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponType()).willReturn(CouponType.AUTO);
        given(policy.getAutoIssueType()).willReturn(null);
        return policy;
    }
}
