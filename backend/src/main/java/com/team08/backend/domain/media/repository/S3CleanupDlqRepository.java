package com.team08.backend.domain.media.repository;

import com.team08.backend.domain.media.entity.DlqStatus;
import com.team08.backend.domain.media.entity.S3CleanupDlq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface S3CleanupDlqRepository extends JpaRepository<S3CleanupDlq, Long> {
    List<S3CleanupDlq> findByStatusAndNextRetryAtBefore(DlqStatus status, Instant now);
}
