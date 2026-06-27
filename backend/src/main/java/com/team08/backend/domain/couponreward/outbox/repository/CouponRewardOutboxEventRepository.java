package com.team08.backend.domain.couponreward.outbox.repository;

import com.team08.backend.domain.couponreward.outbox.entity.CouponRewardOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRewardOutboxEventRepository extends JpaRepository<CouponRewardOutboxEvent, Long> {

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);

    @Query(
            value = """
                    SELECT id
                    FROM coupon_reward_outbox_events
                    WHERE status = :pendingStatus
                       OR (
                            status = :failedStatus
                            AND (next_retry_at IS NULL OR next_retry_at <= :now)
                       )
                    ORDER BY id ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Long> findRetryableIds(
            @Param("pendingStatus") String pendingStatus,
            @Param("failedStatus") String failedStatus,
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT *
                    FROM coupon_reward_outbox_events
                    WHERE id = :id
                      AND (
                            status = :pendingStatus
                         OR (
                                status = :failedStatus
                                AND (next_retry_at IS NULL OR next_retry_at <= :now)
                            )
                      )
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    Optional<CouponRewardOutboxEvent> findRetryableByIdForUpdateSkipLocked(
            @Param("id") Long id,
            @Param("pendingStatus") String pendingStatus,
            @Param("failedStatus") String failedStatus,
            @Param("now") LocalDateTime now
    );

    @Query(
            value = """
                    SELECT *
                    FROM coupon_reward_outbox_events
                    WHERE id = :id
                    FOR UPDATE
                    """,
            nativeQuery = true
    )
    Optional<CouponRewardOutboxEvent> findByIdForUpdate(@Param("id") Long id);
}
