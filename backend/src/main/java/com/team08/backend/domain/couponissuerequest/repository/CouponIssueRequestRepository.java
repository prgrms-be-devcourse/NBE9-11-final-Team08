package com.team08.backend.domain.couponissuerequest.repository;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CouponIssueRequestRepository extends JpaRepository<CouponIssueRequest, Long> {

    Optional<CouponIssueRequest> findByIssueTypeAndRequestKey(CouponIssueRequestType issueType, String requestKey);

    Page<CouponIssueRequest> findAllByOrderByRequestedAtDesc(Pageable pageable);

    @Modifying
    @Query("""
            UPDATE CouponIssueRequest r
            SET r.successCount = r.successCount + 1
            WHERE r.id = :id
              AND r.status NOT IN ('COMPLETED', 'FAILED', 'CANCELED')
            """)
    int incrementSuccessCount(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE CouponIssueRequest r
            SET r.failedCount = r.failedCount + 1
            WHERE r.id = :id
              AND r.status NOT IN ('COMPLETED', 'FAILED', 'CANCELED')
            """)
    int incrementFailedCount(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE CouponIssueRequest r
            SET r.skippedCount = r.skippedCount + 1
            WHERE r.id = :id
              AND r.status NOT IN ('COMPLETED', 'FAILED', 'CANCELED')
            """)
    int incrementSkippedCount(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE CouponIssueRequest r
            SET r.status = 'COMPLETED',
                r.completedAt = :completedAt
            WHERE r.id = :id
              AND r.status NOT IN ('COMPLETED', 'FAILED', 'CANCELED')
              AND r.requestedCount <= r.successCount + r.failedCount + r.skippedCount
            """)
    int completeIfProcessed(@Param("id") Long id, @Param("completedAt") LocalDateTime completedAt);

    @Modifying
    @Query("""
            UPDATE CouponIssueRequest r
            SET r.status = 'FAILED',
                r.failureReason = :failureReason,
                r.completedAt = :completedAt
            WHERE r.id = :id
              AND r.status NOT IN ('COMPLETED', 'FAILED', 'CANCELED')
            """)
    int markFailed(@Param("id") Long id, @Param("failureReason") String failureReason, @Param("completedAt") LocalDateTime completedAt);
}
