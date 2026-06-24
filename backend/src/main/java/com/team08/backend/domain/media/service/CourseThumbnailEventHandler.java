package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.event.CourseThumbnailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseThumbnailEventHandler {

    private final CourseThumbnailService courseThumbnailService;

    @Async("videoEncodingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanUpOldThumbnailOnCommit(CourseThumbnailEvent event) {
        if (event.oldThumbnail() != null) {
            try {
                courseThumbnailService.deleteThumbnail(event.oldThumbnail());
            } catch (Exception e) {
                log.error("Failed to clean up old thumbnail from S3. key: {}", event.oldThumbnail(), e);
            }
        }
    }

    @Async("videoEncodingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void cleanUpNewThumbnailOnRollback(CourseThumbnailEvent event) {
        log.warn("[트랜잭션 롤백 발생] 데이터베이스 롤백에 따른 S3 정합성을 맞추기 위해 업로드된 신규 썸네일 파일을 물리 삭제합니다. key: {}", event.newS3Key());
        if (event.newS3Key() != null) {
            try {
                courseThumbnailService.deleteThumbnail(event.newS3Key());
            } catch (Exception e) {
                log.error("Failed to rollback uploaded thumbnail from S3. key: {}", event.newS3Key(), e);
            }
        }
    }
}