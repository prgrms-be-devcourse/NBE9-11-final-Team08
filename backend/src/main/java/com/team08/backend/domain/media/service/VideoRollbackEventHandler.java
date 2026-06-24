package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.event.VideoRollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoRollbackEventHandler {

    private final List<MediaEncodingService> mediaEncodingServices;

    @Async("videoEncodingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanUpLeftoverVideos(VideoRollbackEvent event) {
        for (MediaEncodingService service : mediaEncodingServices) {
            try {
                service.deleteEncodedFolder(event.targetDirName(), event.lectureId());
            } catch (Exception e) {
                log.error("Failed to clean up leftover video folder for service: {}", service.getClass().getSimpleName(), e);
            }
        }
    }

    @Async("videoEncodingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void cleanUpLeftoverVideosOnRollback(VideoRollbackEvent event) {
        log.warn("[트랜잭션 롤백 발생] 파일 시스템 및 S3 정합성을 맞추기 위해 인코딩 찌꺼기 폴더를 일괄 정리합니다. folder: {}", event.targetDirName());
        for (MediaEncodingService service : mediaEncodingServices) {
            try {
                service.deleteEncodedFolder(event.targetDirName(), event.lectureId());
            } catch (Exception e) {
                log.error("Failed to rollback leftover video folder for service: {}", service.getClass().getSimpleName(), e);
            }
        }
    }
}