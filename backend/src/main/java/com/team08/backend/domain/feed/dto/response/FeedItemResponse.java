package com.team08.backend.domain.feed.dto.response;

import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.entity.FeedItemType;

import java.time.LocalDateTime;

public record FeedItemResponse(
        Long id,
        Long studyId,
        Long actorId,
        String actorNickname,
        FeedItemType type,
        Long sourceId,
        String content,
        LocalDateTime occurredAt
) {
    public static FeedItemResponse from(FeedItem feedItem, String actorNickname) {
        return new FeedItemResponse(
                feedItem.getId(),
                feedItem.getStudyId(),
                feedItem.getActorId(),
                actorNickname,
                feedItem.getType(),
                feedItem.getSourceId(),
                feedItem.getContent(),
                feedItem.getOccurredAt()
        );
    }
}
