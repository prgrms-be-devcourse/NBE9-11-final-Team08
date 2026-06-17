package com.team08.backend.domain.feed.domain;

import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.entity.FeedItemType;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedItemTest {

    @Test
    void studyActivityCreatedEvent로부터_FeedItem을_생성한다() {
        // given
        Long studyActivityId = 10L;
        Long studyId = 1L;
        Long authorId = 3L;
        String summaryContent = "오늘 학습한 내용을 공유합니다.";
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 16, 10, 0);

        StudyActivityCreatedEvent event = new StudyActivityCreatedEvent(
                studyActivityId,
                studyId,
                authorId,
                summaryContent,
                occurredAt
        );

        // when
        FeedItem feedItem = FeedItem.createStudyActivity(
                event.studyId(),
                event.authorId(),
                event.studyActivityId(),
                event.content(),
                event.createdAt()
        );

        // then
        assertThat(feedItem.getStudyId()).isEqualTo(studyId);
        assertThat(feedItem.getAuthorId()).isEqualTo(authorId);
        assertThat(feedItem.getType()).isEqualTo(FeedItemType.STUDY_ACTIVITY);
        assertThat(feedItem.getSourceId()).isEqualTo(studyActivityId);
        assertThat(feedItem.getContent()).isEqualTo(summaryContent);
        assertThat(feedItem.getOccurredAt()).isEqualTo(occurredAt);
    }
}