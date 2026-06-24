package com.team08.backend.domain.feed.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.feed.outbox.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class FeedOutboxScheduler {

    private final FeedOutboxPublisher feedOutboxPublisher;

    @Scheduled(fixedDelayString = "${app.feed.outbox.publish-delay-ms:1000}")
    public void publishPending() {
        feedOutboxPublisher.publishPending();
    }
}
