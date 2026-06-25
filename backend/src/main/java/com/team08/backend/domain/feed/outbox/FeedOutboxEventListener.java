package com.team08.backend.domain.feed.outbox;

import com.team08.backend.domain.studyactivity.event.StudyActivityCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedOutboxEventListener {

    private final FeedOutboxService feedOutboxService;

    @EventListener
    public void onStudyActivityCreated(StudyActivityCreated event) {
        feedOutboxService.createStudyActivityCreatedEvent(event);
    }
}
