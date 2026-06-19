package com.team08.backend.domain.feed.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feed_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"type", "source_id"}),
        indexes = {
                @Index(
                        name = "idx_study_feed",
                        columnList = "study_id, occurred_at, id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studyId;

    @Column(nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedItemType type;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private FeedItem(Long studyId, Long actorId, FeedItemType type, Long sourceId, String content, LocalDateTime occurredAt) {
        this.studyId = studyId;
        this.actorId = actorId;
        this.type = type;
        this.sourceId = sourceId;
        this.content = content;
        this.occurredAt = occurredAt;
    }

    public static FeedItem createStudyActivity(Long studyId, Long authorId, Long activityId, String contentSummary, LocalDateTime occurredAt) {
        return new FeedItem(studyId, authorId, FeedItemType.STUDY_ACTIVITY, activityId, contentSummary, occurredAt);
    }
}
