package com.team08.backend.domain.couponreward.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.couponreward.outbox.dto.AttendanceRewardPayload;
import com.team08.backend.domain.couponreward.outbox.dto.SignupRewardPayload;
import com.team08.backend.domain.couponreward.outbox.entity.CouponRewardOutboxEvent;
import com.team08.backend.domain.couponreward.outbox.entity.CouponRewardOutboxEventStatus;
import com.team08.backend.domain.couponreward.outbox.repository.CouponRewardOutboxEventRepository;
import com.team08.backend.domain.couponreward.service.CouponRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CouponRewardOutboxTransactionService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_BASE_DELAY_SECONDS = 10;
    private static final long RETRY_MAX_DELAY_SECONDS = 600;

    private final CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;
    private final CouponRewardService couponRewardService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueAndMarkProcessed(Long eventId) {
        LocalDateTime now = LocalDateTime.now(clock);
        couponRewardOutboxEventRepository.findRetryableByIdForUpdateSkipLocked(
                        eventId,
                        CouponRewardOutboxEventStatus.PENDING.name(),
                        CouponRewardOutboxEventStatus.FAILED.name(),
                        now
                )
                .ifPresent(this::issueAndMarkProcessed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long eventId, RuntimeException cause) {
        couponRewardOutboxEventRepository.findByIdForUpdate(eventId)
                .filter(event -> event.getStatus() == CouponRewardOutboxEventStatus.PENDING
                        || event.getStatus() == CouponRewardOutboxEventStatus.FAILED)
                .ifPresent(event -> event.markFailed(
                        failureMessage(cause),
                        LocalDateTime.now(clock),
                        MAX_RETRIES,
                        retryDelaySeconds(event.getRetryCount())
                ));
    }

    private void issueAndMarkProcessed(CouponRewardOutboxEvent event) {
        if (CouponRewardOutboxEvent.USER_SIGNED_UP_EVENT.equals(event.getEventType())) {
            SignupRewardPayload payload = readPayload(event, SignupRewardPayload.class);
            couponRewardService.issueSignupReward(payload.userId());
            event.markProcessed(LocalDateTime.now(clock));
            return;
        }

        if (CouponRewardOutboxEvent.ATTENDANCE_CHECKED_EVENT.equals(event.getEventType())) {
            AttendanceRewardPayload payload = readPayload(event, AttendanceRewardPayload.class);
            couponRewardService.issueAttendanceStreakReward(payload.userId(), payload.consecutiveDays());
            couponRewardService.issueMonthlyAttendanceReward(
                    payload.userId(),
                    YEAR_MONTH_FORMATTER.format(payload.attendanceDate()),
                    payload.monthlyTotalDays()
            );
            event.markProcessed(LocalDateTime.now(clock));
            return;
        }

        throw new IllegalStateException("지원하지 않는 쿠폰 보상 outbox 이벤트입니다. eventType=" + event.getEventType());
    }

    private <T> T readPayload(CouponRewardOutboxEvent event, Class<T> type) {
        try {
            return objectMapper.readValue(event.getPayload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("쿠폰 보상 outbox payload 역직렬화 실패", e);
        }
    }

    private String failureMessage(RuntimeException cause) {
        if (cause.getMessage() != null) {
            return cause.getMessage();
        }
        return cause.getClass().getSimpleName();
    }

    private long retryDelaySeconds(int retryCount) {
        long multiplier = 1L << Math.min(retryCount, 30);
        long delay = RETRY_BASE_DELAY_SECONDS * multiplier;
        return Math.min(delay, RETRY_MAX_DELAY_SECONDS);
    }
}
