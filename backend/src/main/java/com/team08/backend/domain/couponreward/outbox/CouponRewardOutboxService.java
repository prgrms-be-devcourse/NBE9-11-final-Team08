package com.team08.backend.domain.couponreward.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CouponRewardOutboxService {

    private final CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void createUserSignedUpEvent(Long userId) {
        SignupRewardPayload payload = new SignupRewardPayload(userId);
        save(CouponRewardOutboxEvent.userSignedUp(userId, writePayload(payload)));
    }

    public void createAttendanceCheckedEvent(
            Long userId,
            LocalDate attendanceDate,
            int consecutiveDays,
            int monthlyTotalDays
    ) {
        AttendanceRewardPayload payload = new AttendanceRewardPayload(
                userId,
                attendanceDate,
                consecutiveDays,
                monthlyTotalDays
        );
        save(CouponRewardOutboxEvent.attendanceChecked(userId, attendanceDate, writePayload(payload)));
    }

    private void save(CouponRewardOutboxEvent event) {
        if (couponRewardOutboxEventRepository.existsByEventTypeAndEventKey(event.getEventType(), event.getEventKey())) {
            return;
        }
        CouponRewardOutboxEvent savedEvent = couponRewardOutboxEventRepository.save(event);
        eventPublisher.publishEvent(new CouponRewardOutboxCreatedEvent(savedEvent.getId()));
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("쿠폰 보상 outbox payload 직렬화 실패", e);
        }
    }
}
