package com.team08.backend.domain.feed.domain;

import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.entity.FeedItemType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedItemTest {

    @Test
    void studyActivity_정보로부터_FeedItem을_생성한다() {
        // given
        Long studyActivityId = 10L;
        Long studyId = 1L;
        Long authorId = 3L;
        String summaryContent = "오늘 학습한 내용을 공유합니다.";
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 16, 10, 0);

        // when
        FeedItem feedItem = FeedItem.createStudyActivity(
                studyId,
                authorId,
                studyActivityId,
                summaryContent,
                occurredAt
        );

        // then
        assertThat(feedItem.getStudyId()).isEqualTo(studyId);
        assertThat(feedItem.getActorId()).isEqualTo(authorId);
        assertThat(feedItem.getType()).isEqualTo(FeedItemType.STUDY_ACTIVITY);
        assertThat(feedItem.getSourceId()).isEqualTo(studyActivityId);
        assertThat(feedItem.getContent()).isEqualTo(summaryContent);
        assertThat(feedItem.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void learningEvent_정보로부터_FeedItem을_생성한다() {
        Long learningEventId = 20L;
        Long studyId = 1L;
        Long actorId = 3L;
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 16, 10, 0);

        FeedItem feedItem = FeedItem.createLearningEvent(
                studyId,
                actorId,
                FeedItemType.LECTURE_ENTER,
                learningEventId,
                "강의에 입장했어요: 스프링 이벤트 기초",
                occurredAt
        );

        assertThat(feedItem.getStudyId()).isEqualTo(studyId);
        assertThat(feedItem.getActorId()).isEqualTo(actorId);
        assertThat(feedItem.getType()).isEqualTo(FeedItemType.LECTURE_ENTER);
        assertThat(feedItem.getSourceId()).isEqualTo(learningEventId);
        assertThat(feedItem.getContent()).isEqualTo("강의에 입장했어요: 스프링 이벤트 기초");
        assertThat(feedItem.getOccurredAt()).isEqualTo(occurredAt);
    }
}
