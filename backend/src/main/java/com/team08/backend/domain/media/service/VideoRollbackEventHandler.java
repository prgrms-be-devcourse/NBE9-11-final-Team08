package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.S3CleanupDlq;
import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.domain.media.repository.S3CleanupDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoRollbackEventHandler {

    private final VideoCleanupService videoCleanupService;
    private final S3CleanupDlqRepository dlqRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanUpOldVideos(VideoCleanUpEvent event) {
        try {
            videoCleanupService.cleanUpOldVideosAsync(event);
        } catch (RejectedExecutionException e) {
            log.error("[구영상 정리 거절] 스레드 풀 포화로 삭제 작업 거절 → DLQ 격리. dir={}", event.targetDirName());
            dlqRepository.save(S3CleanupDlq.ofRejected(event.lectureId(), event.targetDirName()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void cleanUpLeftoverVideosOnRollback(VideoRollbackEvent event) {
        log.warn("[트랜잭션 롤백 발생] 파일 시스템 및 S3 정합성을 맞추기 위해 인코딩 찌꺼기 폴더 정리를 요청합니다. folder: {}", event.targetDirName());
        try {
            videoCleanupService.cleanUpLeftoverVideosOnRollbackAsync(event);
        } catch (RejectedExecutionException e) {
            log.error("[찌꺼기 정리 거절] 스레드 풀 포화로 삭제 작업 거절 → DLQ 격리. dir={}", event.targetDirName());
            dlqRepository.save(S3CleanupDlq.ofRejected(event.lectureId(), event.targetDirName()));
        }
    }
}