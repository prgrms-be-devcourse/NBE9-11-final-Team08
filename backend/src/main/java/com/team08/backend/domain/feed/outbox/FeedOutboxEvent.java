package com.team08.backend.domain.feed.outbox;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feed_item_outbox_events",
        indexes = {
                @Index(name = "idx_feed_outbox_status_id", columnList = "status, id"),
                @Index(name = "idx_feed_outbox_study_id", columnList = "study_id, id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_type", "source_id"}),
                @UniqueConstraint(columnNames = "feed_item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedOutboxEvent extends BaseTimeEntity {

    public static final String FEED_ITEM_CREATED_EVENT = "feed-item.created";
    public static final String STUDY_ACTIVITY_CREATED_EVENT = "study-activity.created";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studyId;

    @Column(nullable = false)
    private Long sourceId;

    private Long feedItemId;

    @Column(nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedOutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Lob
    private String lastError;

    private LocalDateTime publishedAt;

    private FeedOutboxEvent(Long studyId, Long sourceId, String eventType, String payload) {
        this.studyId = studyId;
        this.sourceId = sourceId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = FeedOutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public static FeedOutboxEvent studyActivityCreated(Long studyId, Long studyActivityId, String payload) {
        return new FeedOutboxEvent(studyId, studyActivityId, STUDY_ACTIVITY_CREATED_EVENT, payload);
    }

    public void markPublished(Long feedItemId, String payload, LocalDateTime publishedAt) {
        this.feedItemId = feedItemId;
        this.payload = payload;
        this.status = FeedOutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markPublished(LocalDateTime publishedAt) {
        this.status = FeedOutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public String sseEventName() {
        if (feedItemId != null) {
            return FEED_ITEM_CREATED_EVENT;
        }

        return eventType;
    }

    public void markFailed(String message) {
        this.status = FeedOutboxEventStatus.FAILED;
        this.retryCount++;
        this.lastError = message;
    }
}
