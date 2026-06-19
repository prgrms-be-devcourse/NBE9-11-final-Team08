package com.team08.backend.domain.feed.repository;

import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class FeedItemRepositoryTest {

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Test
    void 첫_페이지는_스터디_피드를_최신순으로_조회한다() {
        LocalDateTime oldTime = LocalDateTime.of(2026, 6, 17, 10, 0);
        LocalDateTime latestTime = LocalDateTime.of(2026, 6, 17, 11, 0);
        FeedItem old = save(1L, 1L, 101L, oldTime);
        FeedItem lowerId = save(1L, 1L, 102L, latestTime);
        FeedItem higherId = save(1L, 1L, 103L, latestTime);
        save(2L, 1L, 104L, latestTime);

        List<FeedItem> result = feedItemRepository.findByStudyIdWithCursor(
                1L,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result)
                .extracting(FeedItem::getId)
                .containsExactly(higherId.getId(), lowerId.getId(), old.getId());
    }

    @Test
    void 커서_다음_페이지는_커서보다_오래된_피드만_조회한다() {
        LocalDateTime latestTime = LocalDateTime.of(2026, 6, 17, 12, 0);
        LocalDateTime sameTime = LocalDateTime.of(2026, 6, 17, 11, 0);
        LocalDateTime oldTime = LocalDateTime.of(2026, 6, 17, 10, 0);
        save(1L, 1L, 101L, latestTime);
        FeedItem sameTimeLowerId = save(1L, 1L, 102L, sameTime);
        FeedItem cursor = save(1L, 1L, 103L, sameTime);
        FeedItem old = save(1L, 1L, 104L, oldTime);

        List<FeedItem> result = feedItemRepository.findByStudyIdWithCursor(
                1L,
                cursor.getOccurredAt(),
                cursor.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result)
                .extracting(FeedItem::getId)
                .containsExactly(sameTimeLowerId.getId(), old.getId());
    }

    private FeedItem save(Long studyId, Long actorId, Long sourceId, LocalDateTime occurredAt) {
        return feedItemRepository.saveAndFlush(
                FeedItem.createStudyActivity(
                        studyId,
                        actorId,
                        sourceId,
                        "스터디 활동 내용",
                        occurredAt
                )
        );
    }
}
