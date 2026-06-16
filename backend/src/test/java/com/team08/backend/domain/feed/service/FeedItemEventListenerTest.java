package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feeditem.entity.FeedItem;
import com.team08.backend.domain.feeditem.entity.FeedItemType;
import com.team08.backend.domain.feeditem.repository.FeedItemRepository;
import com.team08.backend.domain.feeditem.service.FeedContentSummarizer;
import com.team08.backend.domain.feeditem.service.FeedItemEventListener;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FeedItemEventListenerTest {

    @Mock
    private FeedItemRepository feedItemRepository;

    @Mock
    private FeedContentSummarizer feedContentSummarizer;

    @InjectMocks
    private FeedItemEventListener feedItemEventListener;

    @Test
    void StudyActivityCreatedEvent를_받으면_FeedItem을_저장한다() {
        // given
        Long studyId = 1L;
        Long authorId = 1L;
        String content = "오늘 학습한 내용을 공유합니다.";
        StudyActivity studyActivity = StudyActivity.create(studyId, authorId, content);
        StudyActivityCreatedEvent event = StudyActivityCreatedEvent.from(studyActivity);

        given(feedContentSummarizer.summarize(content))
                .willReturn(content);

        // when
        feedItemEventListener.handle(event);

        // then
        ArgumentCaptor<FeedItem> captor = ArgumentCaptor.forClass(FeedItem.class);
        verify(feedItemRepository).save(captor.capture());

        FeedItem saved = captor.getValue();

        assertThat(saved.getStudyId()).isEqualTo(studyId);
        assertThat(saved.getAuthorId()).isEqualTo(authorId);
        assertThat(saved.getType()).isEqualTo(FeedItemType.STUDY_ACTIVITY);
        assertThat(saved.getSourceId()).isEqualTo(studyActivity.getId());
        assertThat(saved.getContent()).isEqualTo(content);
    }
}
