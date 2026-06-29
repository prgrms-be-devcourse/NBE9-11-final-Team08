package com.team08.backend.domain.media.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.springframework.lang.Nullable;

@Entity
@Table(name = "s3_cleanup_dlq", indexes = {
        @Index(name = "idx_s3_cleanup_dlq_status_retry", columnList = "status, next_retry_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class S3CleanupDlq extends BaseTimeEntity {

    public static final int MAX_RETRY = 3;
    private static final long[] BACKOFF_SECONDS = { 120L, 240L, 480L };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lectureId;

    @Column(nullable = false)
    private String targetDirName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DlqStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private Instant failedAt;

    @Nullable
    private Instant nextRetryAt;

    private S3CleanupDlq(Long lectureId, String targetDirName) {
        this.lectureId = lectureId;
        this.targetDirName = targetDirName;
        this.status = DlqStatus.PENDING;
        this.retryCount = 0;
        this.failedAt = Instant.now();
        this.nextRetryAt = Instant.now().plusSeconds(60);
    }

    public static S3CleanupDlq ofRejected(Long lectureId, String targetDirName) {
        return new S3CleanupDlq(lectureId, targetDirName);
    }

    public void incrementRetry(String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;
        this.failedAt = Instant.now();

        if (this.retryCount >= MAX_RETRY) {
            this.status = DlqStatus.DEAD;
            this.nextRetryAt = null;
        } else {
            long backoff = BACKOFF_SECONDS[Math.min(this.retryCount - 1, BACKOFF_SECONDS.length - 1)];
            this.nextRetryAt = Instant.now().plusSeconds(backoff);
        }
    }

    public void markDead(String reason) {
        this.status = DlqStatus.DEAD;
        this.lastError = reason;
        this.nextRetryAt = null;
    }
}
