package com.team08.backend.domain.issuedcouponjob.repository;

import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface IssuedCouponJobRepository extends JpaRepository<IssuedCouponJob, Long> {
    // 특정 사용자의 쿠폰 발급 작업 조회
    Optional<IssuedCouponJob> findByIdAndUserId(Long id, Long userId);

    // 처리 가능한 작업 하나만 PROCESSING 상태로 선점
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE IssuedCouponJob j
            SET j.status = :processingStatus,
                j.lastTriedAt = :startedAt
            WHERE j.id = :jobId
              AND j.status IN :processableStatuses
            """)
    int markProcessing(
            @Param("jobId") Long jobId,
            @Param("processingStatus") IssuedCouponJobStatus processingStatus,
            @Param("processableStatuses") Collection<IssuedCouponJobStatus> processableStatuses,
            @Param("startedAt") LocalDateTime startedAt
    );
}
