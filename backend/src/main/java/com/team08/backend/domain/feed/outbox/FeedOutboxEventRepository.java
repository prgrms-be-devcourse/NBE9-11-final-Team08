package com.team08.backend.domain.feed.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedOutboxEventRepository extends JpaRepository<FeedOutboxEvent, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM feed_item_outbox_events
                    WHERE status IN (:statuses)
                    ORDER BY id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<FeedOutboxEvent> findRetryableForUpdateSkipLocked(
            @Param("statuses") List<String> statuses,
            @Param("limit") int limit
    );

    List<FeedOutboxEvent> findByStudyIdAndStatusAndIdGreaterThanOrderByIdAsc(
            Long studyId,
            FeedOutboxEventStatus status,
            Long id
    );
}
