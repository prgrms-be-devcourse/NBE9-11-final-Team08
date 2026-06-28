package com.team08.backend.domain.media.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 비동기 S3 삭제 태스크가 RejectedExecutionException 혹은 삭제 실패로 소실되지 않도록
 * 영구 보존하는 DB 기반 Dead Letter Queue 엔티티입니다.
 *
 * - 스레드 풀 포화 → videoCleanupExecutor RejectedExecutionHandler 에서 즉시 save
 * - 삭제 자체 실패 → VideoRollbackEventHandler 예외 핸들러에서 즉시 save
 * - S3CleanupDlqDrainer 가 1분 주기로 PENDING 항목을 꺼내 지수 백오프 재시도
 * - MAX_RETRY(3) 초과 시 DEAD 상태로 전환 → 수동 처리 필요 알람 발화
 */
@Entity
@Table(name = "s3_cleanup_dlq", indexes = {
        @Index(name = "idx_s3_cleanup_dlq_status_retry", columnList = "status, next_retry_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class S3CleanupDlq extends BaseTimeEntity {

    public static final int MAX_RETRY = 3;

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

    private Instant nextRetryAt;

    private S3CleanupDlq(Long lectureId, String targetDirName) {
        this.lectureId = lectureId;
        this.targetDirName = targetDirName;
        this.status = DlqStatus.PENDING;
        this.retryCount = 0;
        this.failedAt = Instant.now();
        this.nextRetryAt = Instant.now(); // 즉시 재시도 가능
    }

    /**
     * 스레드 풀 거절(Rejected) 또는 삭제 실패 발생 시 DLQ 항목을 생성합니다.
     */
    public static S3CleanupDlq ofRejected(Long lectureId, String targetDirName) {
        return new S3CleanupDlq(lectureId, targetDirName);
    }

    /**
     * 재시도 실패 시 호출합니다.
     * retryCount를 1 증가시키고, 지수 백오프 공식으로 next_retry_at을 계산합니다.
     * MAX_RETRY 초과 시 DEAD로 전환합니다.
     *
     * 백오프 공식: now + 60 * 2^retryCount (초)
     *   - 1차 실패: 2분 후 재시도
     *   - 2차 실패: 4분 후 재시도
     *   - 3차 초과: DEAD 전환
     */
    public void incrementRetry(String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;
        this.failedAt = Instant.now();

        if (this.retryCount >= MAX_RETRY) {
            this.status = DlqStatus.DEAD;
            this.nextRetryAt = null;
        } else {
            long backoffSeconds = 60L * (1L << this.retryCount); // 2^retryCount * 60초
            this.nextRetryAt = Instant.now().plusSeconds(backoffSeconds);
        }
    }

    /**
     * 명시적으로 DEAD 상태로 전환합니다.
     */
    public void markDead(String reason) {
        this.status = DlqStatus.DEAD;
        this.lastError = reason;
        this.nextRetryAt = null;
    }
}
