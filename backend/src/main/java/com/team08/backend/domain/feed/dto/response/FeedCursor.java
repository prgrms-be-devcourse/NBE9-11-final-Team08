package com.team08.backend.domain.feed.dto.response;

import java.time.LocalDateTime;

public record FeedCursor(
        LocalDateTime occurredAt,
        Long id
) {
}
