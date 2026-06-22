package com.team08.backend.domain.feed.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedSseDrainListener {

    private static final String SERVER_DRAINING_REASON = "server-draining";

    private final FeedSseConnectionManager feedSseConnectionManager;

    @EventListener
    public void onContextClosed(ContextClosedEvent ignored) {
        feedSseConnectionManager.drainAll(SERVER_DRAINING_REASON);
    }
}
