package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class FeedServiceTest {

    @Mock
    StudyRepository studyRepository;

    @Mock
    StudyMemberRepository studyMemberRepository;

    @Mock
    FeedItemRepository feedItemRepository;

    @InjectMocks
    FeedService feedService;

    @Test
    void activeMember는_스터디_피드를_조회할_수_있다() {
        // given
        Long studyId = 1L;
        Long activityId = 100L;
        String content = "스터디 활동 내용";
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 17, 10, 0);

        User user = UserFixture.builder().build();
        Long userId = user.getId();

        Pageable pageable = PageRequest.of(0, 10);

        Pageable latestFirstPageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Order.desc("occurredAt"),
                        Sort.Order.desc("id")
                )
        );

        FeedItem feedItem = FeedItem.createStudyActivity(
                studyId,
                userId,
                activityId,
                content,
                occurredAt
        );

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(true);

        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(true);

        given(feedItemRepository.findAllByStudyId(studyId, latestFirstPageable))
                .willReturn(new PageImpl<>(List.of(feedItem), latestFirstPageable, 1));

        // when
        Page<FeedItemResponse> result = feedService.getPagedFeedItems(studyId, userId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).sourceId()).isEqualTo(activityId);
        assertThat(result.getContent().get(0).studyId()).isEqualTo(studyId);
        assertThat(result.getContent().get(0).actorId()).isEqualTo(userId);
        assertThat(result.getContent().get(0).content()).isEqualTo(content);

        then(feedItemRepository).should()
                .findAllByStudyId(studyId, latestFirstPageable);
    }

    @Test
    void activeMember가_아니면_피드를_조회할_수_없다() {
        // given
        Long studyId = 1L;
        Long userId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(true);

        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId,
                userId,
                StudyMemberStatus.ACTIVE
        )).willReturn(false);

        // when & then
        assertThatThrownBy(() -> feedService.getPagedFeedItems(studyId, userId, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.STUDY_ACCESS_DENIED.getMessage());

        then(feedItemRepository).shouldHaveNoInteractions();
    }

    @Test
    void draft_또는_inactiveStudy는_피드를_조회할_수_없다() {
        // given
        Long studyId = 1L;
        Long userId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> feedService.getPagedFeedItems(studyId, userId, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.STUDY_ACCESS_DENIED.getMessage());

        then(studyMemberRepository).shouldHaveNoInteractions();
        then(feedItemRepository).shouldHaveNoInteractions();
    }
}
