package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.DlqStatus;
import com.team08.backend.domain.media.entity.S3CleanupDlq;
import com.team08.backend.domain.media.repository.S3CleanupDlqRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupDlqDrainer {

    private final S3CleanupDlqRepository dlqRepository;
    private final DlqTaskProcessor taskProcessor;

    @Scheduled(fixedDelay = 60_000)
    public void drainDlq() {
        List<S3CleanupDlq> pendingTasks =
                dlqRepository.findByStatusAndNextRetryAtBefore(DlqStatus.PENDING, Instant.now());

        if (pendingTasks.isEmpty()) {
            return;
        }

        log.info("[DLQ 드레인 시작] 처리 대상 항목: {}건", pendingTasks.size());

        for (S3CleanupDlq task : pendingTasks) {
            taskProcessor.processTask(task);
        }
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class DlqTaskProcessor {

    private final S3CleanupDlqRepository dlqRepository;
    private final List<MediaEncodingService> mediaEncodingServices;
    private final MeterRegistry meterRegistry;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTask(S3CleanupDlq task) {
        boolean success = false;
        StringBuilder errorBuilder = new StringBuilder();

        for (MediaEncodingService service : mediaEncodingServices) {
            try {
                service.deleteEncodedFolder(task.getTargetDirName(), task.getLectureId());
                success = true;
                log.info("[DLQ 재시도 성공] lectureId={}, dir={}, retryCount={}",
                        task.getLectureId(), task.getTargetDirName(), task.getRetryCount());
            } catch (Exception e) {
                log.error("[DLQ 재시도 실패] lectureId={}, dir={}, retryCount={}, service={}",
                        task.getLectureId(), task.getTargetDirName(), task.getRetryCount(),
                        service.getClass().getSimpleName(), e);
                if (errorBuilder.length() > 0) {
                    errorBuilder.append(" | ");
                }
                errorBuilder.append(service.getClass().getSimpleName()).append(": ").append(e.getMessage());
            }
        }

        if (success) {
            dlqRepository.delete(task);
            meterRegistry.counter("dlq.drain.success").increment();
        } else {
            task.incrementRetry(errorBuilder.toString());
            dlqRepository.save(task);
            if (task.getStatus() == DlqStatus.DEAD) {
                log.error("[DLQ DEAD 전환] 최대 재시도 초과. 수동 처리 필요! lectureId={}, dir={}",
                        task.getLectureId(), task.getTargetDirName());
                meterRegistry.counter("dlq.dead.count",
                        "lectureId", String.valueOf(task.getLectureId())).increment();
            }
        }
    }
}
