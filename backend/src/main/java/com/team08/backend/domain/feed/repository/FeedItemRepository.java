package com.team08.backend.domain.feed.repository;

import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.entity.FeedItemType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {

    @Query("""
            SELECT f
            FROM FeedItem f
            WHERE f.studyId = :studyId
              AND (
                    :cursorOccurredAt IS NULL
                    OR :cursorId IS NULL
                    OR f.occurredAt < :cursorOccurredAt
                    OR (f.occurredAt = :cursorOccurredAt AND f.id < :cursorId)
              )
            ORDER BY f.occurredAt DESC, f.id DESC
            """)
    List<FeedItem> findByStudyIdWithCursor(
            @Param("studyId") Long studyId,
            @Param("cursorOccurredAt") LocalDateTime cursorOccurredAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    Optional<FeedItem> findByTypeAndSourceId(FeedItemType type, Long sourceId);
}
