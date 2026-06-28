package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.DlqStatus;
import com.team08.backend.domain.media.entity.S3CleanupDlq;
import com.team08.backend.domain.media.repository.S3CleanupDlqRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * s3_cleanup_dlq 테이블에 쌓인 실패 항목을 1분 주기로 드레인하여
 * 지수 백오프 전략으로 S3(또는 로컬 파일시스템) 삭제를 재시도합니다.
 *
 * 재시도 전략:
 * - 1차 실패 → 2분 후 재시도
 * - 2차 실패 → 4분 후 재시도
 * - 3차(MAX_RETRY) 초과 → DEAD 상태 전환 + Micrometer 카운터 발화 (Grafana 알람)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupDlqDrainer {

    private final S3CleanupDlqRepository dlqRepository;
    private final List<MediaEncodingService> mediaEncodingServices;
    private final MeterRegistry meterRegistry;

    @Transactional
    @Scheduled(fixedDelay = 60_000) // 1분 주기
    public void drainDlq() {
        List<S3CleanupDlq> pendingTasks =
                dlqRepository.findByStatusAndNextRetryAtBefore(DlqStatus.PENDING, Instant.now());

        if (pendingTasks.isEmpty()) {
            return;
        }

        log.info("[DLQ 드레인 시작] 처리 대상 항목: {}건", pendingTasks.size());

        for (S3CleanupDlq task : pendingTasks) {
            boolean success = false;

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
                    task.incrementRetry(e.getMessage());

                    if (task.getStatus() == DlqStatus.DEAD) {
                        log.error("[DLQ DEAD 전환] 최대 재시도 초과. 수동 처리 필요! lectureId={}, dir={}",
                                task.getLectureId(), task.getTargetDirName());
                        meterRegistry.counter("dlq.dead.count",
                                "lectureId", String.valueOf(task.getLectureId())).increment();
                    }
                }
            }

            if (success) {
                dlqRepository.delete(task);
                meterRegistry.counter("dlq.drain.success").increment();
            }
        }
    }
}
