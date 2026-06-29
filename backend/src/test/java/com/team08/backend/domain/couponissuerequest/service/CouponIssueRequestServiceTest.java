package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.repository.CouponIssueRequestRepository;
import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CouponIssueRequestServiceTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private CouponIssueRequestRepository couponIssueRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponIssueRequestStreamPublisher streamPublisher;

    private CouponIssueRequestService couponIssueRequestService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        couponIssueRequestService = new CouponIssueRequestService(
                couponPolicyRepository,
                couponIssueRequestRepository,
                userRepository,
                issuedCouponRepository,
                streamPublisher,
                clock
        );
    }

    @Test
    @DisplayName("특정 회원 발급 요청을 저장하고 회원별 Redis Stream 메시지를 발행한다")
    void requestUsersIssue_savesRequestAndPublishesMessages() {
        // given
        Long policyId = 10L;
        Long adminId = 99L;
        CouponPolicy policy = manualIssuePolicy();
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));
        given(userRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(mock(User.class), mock(User.class)));
        given(issuedCouponRepository.findIssuedUserIds(policyId, List.of(1L, 2L))).willReturn(List.of());
        given(couponIssueRequestRepository.saveAndFlush(any(CouponIssueRequest.class)))
                .willAnswer(invocation -> {
                    CouponIssueRequest request = invocation.getArgument(0);
                    ReflectionTestUtils.setField(request, "id", 100L);
                    return request;
                });

        // when
        couponIssueRequestService.requestUsersIssue(
                policyId,
                List.of(1L, 2L, 1L),
                "VIP_EVENT_2026",
                adminId
        );

        // then
        ArgumentCaptor<CouponIssueRequest> requestCaptor = ArgumentCaptor.forClass(CouponIssueRequest.class);
        then(couponIssueRequestRepository).should().saveAndFlush(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getPolicyId()).isEqualTo(policyId);
        assertThat(requestCaptor.getValue().getRequestKey()).isEqualTo("VIP_EVENT_2026");
        assertThat(requestCaptor.getValue().getRequestedCount()).isEqualTo(2);
        assertThat(requestCaptor.getValue().getRequestedBy()).isEqualTo(adminId);

        then(streamPublisher).should().publishAll(100L, policyId, List.of(1L, 2L), "SELECTED_USERS_VIP_EVENT_2026");
    }

    @Test
    @DisplayName("이미 같은 정책 쿠폰을 받은 회원은 스킵 처리하고 메시지를 발행하지 않는다")
    void requestUsersIssue_skipsAlreadyIssuedUsers() {
        // given
        Long policyId = 10L;
        Long adminId = 99L;
        CouponPolicy policy = manualIssuePolicy();
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));
        given(userRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(mock(User.class), mock(User.class)));
        given(issuedCouponRepository.findIssuedUserIds(policyId, List.of(1L, 2L))).willReturn(List.of(1L));
        given(couponIssueRequestRepository.saveAndFlush(any(CouponIssueRequest.class)))
                .willAnswer(invocation -> {
                    CouponIssueRequest request = invocation.getArgument(0);
                    ReflectionTestUtils.setField(request, "id", 100L);
                    return request;
                });

        // when
        couponIssueRequestService.requestUsersIssue(
                policyId,
                List.of(1L, 2L),
                "VIP_EVENT_2026",
                adminId
        );

        // then
        ArgumentCaptor<CouponIssueRequest> requestCaptor = ArgumentCaptor.forClass(CouponIssueRequest.class);
        then(couponIssueRequestRepository).should().saveAndFlush(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRequestedCount()).isEqualTo(2);
        assertThat(requestCaptor.getValue().getSkippedCount()).isEqualTo(1);

        then(streamPublisher).should().publishAll(100L, policyId, List.of(2L), "SELECTED_USERS_VIP_EVENT_2026");
    }

    @Test
    @DisplayName("ADMIN_ISSUE 타입이 아니면 특정 회원 발급 요청을 거부한다")
    void requestUsersIssue_rejectsNonManualIssuePolicy() {
        // given
        Long policyId = 10L;
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponType()).willReturn(CouponType.AUTO);
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));

        // when & then
        assertThatThrownBy(() -> couponIssueRequestService.requestUsersIssue(
                policyId,
                List.of(1L),
                "VIP_EVENT_2026",
                99L
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_COUPON_TYPE);

        then(couponIssueRequestRepository).shouldHaveNoInteractions();
        then(streamPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청 키가 중복되면 예외가 발생하고 메시지를 발행하지 않는다")
    void requestUsersIssue_throwsWhenRequestKeyDuplicated() {
        // given
        Long policyId = 10L;
        CouponPolicy policy = manualIssuePolicy();
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));
        given(userRepository.findAllById(List.of(1L))).willReturn(List.of(mock(User.class)));
        given(issuedCouponRepository.findIssuedUserIds(policyId, List.of(1L))).willReturn(List.of());
        given(couponIssueRequestRepository.saveAndFlush(any(CouponIssueRequest.class)))
                .willThrow(new DataIntegrityViolationException("duplicated"));

        // when & then
        assertThatThrownBy(() -> couponIssueRequestService.requestUsersIssue(
                policyId,
                List.of(1L),
                "VIP_EVENT_2026",
                99L
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);

        then(streamPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 회원이 포함되면 발급 요청을 저장하지 않는다")
    void requestUsersIssue_throwsWhenUserNotFound() {
        // given
        Long policyId = 10L;
        CouponPolicy policy = manualIssuePolicy();
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));
        given(userRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(mock(User.class)));

        // when & then
        assertThatThrownBy(() -> couponIssueRequestService.requestUsersIssue(
                policyId,
                List.of(1L, 2L),
                "VIP_EVENT_2026",
                99L
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        then(couponIssueRequestRepository).should(never()).saveAndFlush(any(CouponIssueRequest.class));
        then(streamPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("전체 회원 발급 요청은 배치 없이 즉시 grant 완료로 저장한다")
    void requestAllUsersIssue_opensGrantWithoutLaunchingBatch() {
        // given
        Long policyId = 10L;
        Long adminId = 99L;
        CouponPolicy policy = manualIssuePolicy();
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));
        given(userRepository.count()).willReturn(100_000L);
        given(userRepository.findMaxId()).willReturn(Optional.of(123_456L));
        given(couponIssueRequestRepository.saveAndFlush(any(CouponIssueRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        couponIssueRequestService.requestAllUsersIssue(policyId, "ALL_EVENT_2026", adminId);

        // then
        ArgumentCaptor<CouponIssueRequest> requestCaptor = ArgumentCaptor.forClass(CouponIssueRequest.class);
        then(couponIssueRequestRepository).should().saveAndFlush(requestCaptor.capture());
        CouponIssueRequest request = requestCaptor.getValue();
        assertThat(request.getPolicyId()).isEqualTo(policyId);
        assertThat(request.getRequestKey()).isEqualTo("ALL_EVENT_2026");
        assertThat(request.getRequestedCount()).isEqualTo(100_000L);
        assertThat(request.getSuccessCount()).isZero();
        assertThat(request.getFailedCount()).isZero();
        assertThat(request.getSkippedCount()).isZero();
        assertThat(request.getTargetUserMaxId()).isEqualTo(123_456L);
        assertThat(request.getCompletedAt()).isEqualTo(java.time.LocalDateTime.now(clock));
        then(streamPublisher).shouldHaveNoInteractions();
    }

    private CouponPolicy manualIssuePolicy() {
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponType()).willReturn(CouponType.ADMIN_ISSUE);
        return policy;
    }
}
