package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.S3CleanupDlq;
import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.domain.media.repository.S3CleanupDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCleanupService {

    private final List<MediaEncodingService> mediaEncodingServices;
    private final S3CleanupDlqRepository dlqRepository;

    @Async("videoCleanupExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanUpOldVideosAsync(VideoCleanUpEvent event) {
        log.info("[커밋 후 구영상 정리] lectureId={}, folder={}", event.lectureId(), event.targetDirName());
        for (MediaEncodingService service : mediaEncodingServices) {
            try {
                service.deleteEncodedFolder(event.targetDirName(), event.lectureId());
            } catch (Exception e) {
                log.error("[구영상 정리 실패] DLQ에 격리. service={}, dir={}", 
                        service.getClass().getSimpleName(), event.targetDirName(), e);
                dlqRepository.save(S3CleanupDlq.ofRejected(event.lectureId(), event.targetDirName()));
            }
        }
    }

    @Async("videoCleanupExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanUpLeftoverVideosOnRollbackAsync(VideoRollbackEvent event) {
        log.warn("[롤백 후 찌꺼기 정리] lectureId={}, folder={}", event.lectureId(), event.targetDirName());
        for (MediaEncodingService service : mediaEncodingServices) {
            try {
                service.deleteEncodedFolder(event.targetDirName(), event.lectureId());
            } catch (Exception e) {
                log.error("[찌꺼기 정리 실패] DLQ에 격리. service={}, dir={}", 
                        service.getClass().getSimpleName(), event.targetDirName(), e);
                dlqRepository.save(S3CleanupDlq.ofRejected(event.lectureId(), event.targetDirName()));
            }
        }
    }
}
