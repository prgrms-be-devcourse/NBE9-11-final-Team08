package com.team08.backend.domain.feeditem.service;

import com.team08.backend.domain.feeditem.entity.FeedItem;
import com.team08.backend.domain.feeditem.repository.FeedItemRepository;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedItemEventListener {

    private final FeedItemRepository feedItemRepository;
    private final FeedContentSummarizer feedContentSummarizer;

    @EventListener
    public void handle(StudyActivityCreatedEvent event) {
        String summaryContent = feedContentSummarizer.summarize(event.content());

        FeedItem feedItem = FeedItem.createStudyActivity(
                event.studyId(),
                event.authorId(),
                event.studyActivityId(),
                summaryContent,
                event.createdAt()
        );

        feedItemRepository.save(feedItem);
    }
}
