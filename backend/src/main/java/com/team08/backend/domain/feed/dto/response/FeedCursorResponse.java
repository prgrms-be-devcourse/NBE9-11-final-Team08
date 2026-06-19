package com.team08.backend.domain.feed.dto.response;

import java.util.List;

public record FeedCursorResponse(
        List<FeedItemResponse> items,
        FeedCursor nextCursor,
        boolean hasNext
) {
}
