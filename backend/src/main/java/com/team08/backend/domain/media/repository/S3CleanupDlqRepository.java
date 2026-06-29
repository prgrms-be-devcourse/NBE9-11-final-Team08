package com.team08.backend.domain.media.repository;

import com.team08.backend.domain.media.entity.DlqStatus;
import com.team08.backend.domain.media.entity.S3CleanupDlq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface S3CleanupDlqRepository extends JpaRepository<S3CleanupDlq, Long> {

    /**
     * 재시도 가능한 DLQ 항목 조회 - status=PENDING이고 next_retry_at이 현재 시각 이전인 항목
     */
    List<S3CleanupDlq> findByStatusAndNextRetryAtBefore(DlqStatus status, Instant now);
}
