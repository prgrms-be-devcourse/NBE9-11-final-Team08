package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FeedItemEventListener {

    private final FeedItemRepository feedItemRepository;
    private final FeedContentSummarizer feedContentSummarizer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
