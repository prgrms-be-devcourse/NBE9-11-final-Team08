package com.team08.backend.domain.feed.sse;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeedSseDrainListenerTest {

    @Test
    void context가_종료되면_sse_연결을_drain한다() {
        FeedSseConnectionManager feedSseConnectionManager = mock(FeedSseConnectionManager.class);
        FeedSseDrainListener listener = new FeedSseDrainListener(feedSseConnectionManager);

        listener.onContextClosed(mock(ContextClosedEvent.class));

        verify(feedSseConnectionManager).drainAll("server-draining");
    }
}
