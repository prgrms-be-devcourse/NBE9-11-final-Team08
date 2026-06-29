package com.team08.backend.domain.couponissuerequest.repository;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponIssueRequestRepository extends JpaRepository<CouponIssueRequest, Long> {

    Optional<CouponIssueRequest> findByIssueTypeAndRequestKey(CouponIssueRequestType issueType, String requestKey);

    Page<CouponIssueRequest> findAllByOrderByRequestedAtDesc(Pageable pageable);

    @Query("""
            SELECT p
            FROM CouponIssueRequest r
            JOIN CouponPolicy p ON p.id = r.policyId
            WHERE r.issueType = 'ALL_USERS'
              AND r.status = 'COMPLETED'
              AND r.targetUserMaxId >= :userId
              AND p.couponType = 'ADMIN_ISSUE'
              AND (p.issueStartDate IS NULL OR p.issueStartDate <= :now)
              AND (p.issueEndDate IS NULL OR p.issueEndDate >= :now)
              AND NOT EXISTS (
                  SELECT 1
                  FROM IssuedCoupon ic
                  WHERE ic.userId = :userId
                    AND ic.policyId = p.id
              )
            ORDER BY r.completedAt ASC, r.id ASC
            """)
    List<CouponPolicy> findMaterializableAllUsersPolicies(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

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
            SET r.successCount = r.successCount + :successCount,
                r.skippedCount = r.skippedCount + :skippedCount,
                r.failedCount = r.failedCount + :failedCount
            WHERE r.id = :id
              AND r.status NOT IN ('COMPLETED', 'FAILED', 'CANCELED')
            """)
    int incrementProcessCounts(
            @Param("id") Long id,
            @Param("successCount") long successCount,
            @Param("skippedCount") long skippedCount,
            @Param("failedCount") long failedCount
    );

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

    @Transactional
    @Modifying
    @Query("""
            UPDATE CouponIssueRequest r
            SET r.successCount = r.successCount + :count
            WHERE r.policyId = :policyId
              AND r.issueType = 'ALL_USERS'
              AND r.status = 'COMPLETED'
            """)
    int incrementAllUsersSuccessCount(@Param("policyId") Long policyId, @Param("count") long count);
}
