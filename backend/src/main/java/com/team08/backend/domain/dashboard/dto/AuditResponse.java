package com.team08.backend.domain.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학습 데이터 보존 현황 감사(기존 데이터 파생 뷰).
 * retention: 보존 로그 요약. accessHistory: 최근 접근/변경 이력. integrityErrors: 정합성 오류.
 */
public record AuditResponse(
        Retention retention,
        List<AccessHistoryEntry> accessHistory,
        List<IntegrityError> integrityErrors
) {
    public record Retention(
            long learningEventCount,
            LocalDateTime oldestEventTime,
            LocalDateTime newestEventTime,
            long courseStatusHistoryCount,
            long lectureProgressCount
    ) {
    }

    public record AccessHistoryEntry(
            String source,
            String description,
            Long actorId,
            LocalDateTime occurredAt
    ) {
    }

    public record IntegrityError(
            String type,
            String description,
            long count,
            List<Long> sampleIds
    ) {
    }
}
