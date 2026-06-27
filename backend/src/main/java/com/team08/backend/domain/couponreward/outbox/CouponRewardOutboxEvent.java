package com.team08.backend.domain.couponreward.outbox;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupon_reward_outbox_events",
        indexes = {
                @Index(name = "idx_coupon_reward_outbox_status_id", columnList = "status, id"),
                @Index(name = "idx_coupon_reward_outbox_user", columnList = "user_id, id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_coupon_reward_outbox_event_key", columnNames = {"event_type", "event_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRewardOutboxEvent extends BaseTimeEntity {

    public static final String USER_SIGNED_UP_EVENT = "user.signed-up";
    public static final String ATTENDANCE_CHECKED_EVENT = "attendance.checked";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String eventKey;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponRewardOutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String lastError;

    private LocalDateTime processedAt;

    private LocalDateTime nextRetryAt;

    private CouponRewardOutboxEvent(Long userId, String eventType, String eventKey, String payload) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventKey = eventKey;
        this.payload = payload;
        this.status = CouponRewardOutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public static CouponRewardOutboxEvent userSignedUp(Long userId, String payload) {
        return new CouponRewardOutboxEvent(
                userId,
                USER_SIGNED_UP_EVENT,
                String.valueOf(userId),
                payload
        );
    }

    public static CouponRewardOutboxEvent attendanceChecked(Long userId, java.time.LocalDate attendanceDate, String payload) {
        return new CouponRewardOutboxEvent(
                userId,
                ATTENDANCE_CHECKED_EVENT,
                userId + ":" + attendanceDate,
                payload
        );
    }

    public void markProcessed(LocalDateTime processedAt) {
        this.status = CouponRewardOutboxEventStatus.PROCESSED;
        this.processedAt = processedAt;
        this.lastError = null;
        this.nextRetryAt = null;
    }

    public void markFailed(String message, LocalDateTime now, int maxRetries, long retryDelaySeconds) {
        this.retryCount++;
        this.lastError = message;

        if (this.retryCount >= maxRetries) {
            this.status = CouponRewardOutboxEventStatus.DEAD;
            this.nextRetryAt = null;
            return;
        }

        this.status = CouponRewardOutboxEventStatus.FAILED;
        this.nextRetryAt = now.plusSeconds(retryDelaySeconds);
    }
}
