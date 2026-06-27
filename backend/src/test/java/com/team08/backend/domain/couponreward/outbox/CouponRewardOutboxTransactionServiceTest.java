package com.team08.backend.domain.couponreward.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.couponreward.service.CouponRewardService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CouponRewardOutboxTransactionServiceTest {

    @Mock
    private CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;

    @Mock
    private CouponRewardService couponRewardService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-27T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    @DisplayName("회원가입 outbox 이벤트 처리 성공 시 쿠폰을 발급하고 PROCESSED로 변경한다")
    void issueAndMarkProcessed_issuesSignupRewardAndMarksProcessed() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        CouponRewardOutboxProperties properties = new CouponRewardOutboxProperties(100, 5, 10, 600);
        CouponRewardOutboxTransactionService service = new CouponRewardOutboxTransactionService(
                couponRewardOutboxEventRepository,
                couponRewardService,
                objectMapper,
                clock,
                properties
        );
        CouponRewardOutboxEvent event = CouponRewardOutboxEvent.userSignedUp(
                1L,
                objectMapper.writeValueAsString(new SignupRewardPayload(1L))
        );
        LocalDateTime now = LocalDateTime.now(clock);
        given(couponRewardOutboxEventRepository.findRetryableByIdForUpdateSkipLocked(
                10L,
                CouponRewardOutboxEventStatus.PENDING.name(),
                CouponRewardOutboxEventStatus.FAILED.name(),
                now
        )).willReturn(Optional.of(event));

        // when
        service.issueAndMarkProcessed(10L);

        // then
        then(couponRewardService).should().issueSignupReward(1L);
        assertThat(event.getStatus()).isEqualTo(CouponRewardOutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("실패 기록 시 재시도 횟수를 증가시키고 FAILED로 변경한다")
    void markFailed_marksEventFailedWithRetryDelay() {
        // given
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        CouponRewardOutboxProperties properties = new CouponRewardOutboxProperties(100, 5, 10, 600);
        CouponRewardOutboxTransactionService service = new CouponRewardOutboxTransactionService(
                couponRewardOutboxEventRepository,
                couponRewardService,
                objectMapper,
                clock,
                properties
        );
        CouponRewardOutboxEvent event = CouponRewardOutboxEvent.userSignedUp(1L, "{\"userId\":1}");
        given(couponRewardOutboxEventRepository.findByIdForUpdate(10L)).willReturn(Optional.of(event));

        // when
        service.markFailed(10L, new IllegalStateException("failed"));

        // then
        assertThat(event.getStatus()).isEqualTo(CouponRewardOutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("failed");
        assertThat(event.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock).plusSeconds(10));
    }
}
