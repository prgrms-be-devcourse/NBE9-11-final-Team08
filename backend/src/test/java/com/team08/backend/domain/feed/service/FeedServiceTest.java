package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feed.dto.response.FeedCursorResponse;
import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    UserRepository userRepository;

    @InjectMocks
    FeedService feedService;

    @Test
    void activeMember는_스터디_피드를_조회할_수_있다() {
        // given
        Long studyId = 1L;
        Long activityId = 100L;
        String content = "스터디 활동 내용";
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 17, 10, 0);
        int size = 10;

        User user = UserFixture.builder().build();
        Long userId = user.getId();

        FeedItem feedItem = FeedItem.createStudyActivity(
                studyId,
                userId,
                activityId,
                content,
                occurredAt
        );
        ReflectionTestUtils.setField(feedItem, "id", 1L);

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(true);

        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(true);

        given(feedItemRepository.findByStudyIdWithCursor(
                studyId,
                null,
                null,
                PageRequest.of(0, size + 1)
        )).willReturn(List.of(feedItem));
        given(userRepository.findAllById(List.of(userId)))
                .willReturn(List.of(user));

        // when
        FeedCursorResponse result = feedService.getFeedItems(studyId, userId, null, null, size);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();

        FeedItemResponse response = result.items().get(0);
        assertThat(response.sourceId()).isEqualTo(activityId);
        assertThat(response.studyId()).isEqualTo(studyId);
        assertThat(response.actorId()).isEqualTo(userId);
        assertThat(response.actorNickname()).isEqualTo(user.getNickname());
        assertThat(response.content()).isEqualTo(content);

        then(feedItemRepository).should()
                .findByStudyIdWithCursor(studyId, null, null, PageRequest.of(0, size + 1));
    }

    @Test
    void 다음_페이지가_있으면_nextCursor를_반환한다() {
        // given
        Long studyId = 1L;
        Long userId = 2L;
        int size = 2;

        FeedItem first = feedItem(1L, studyId, userId, 101L, LocalDateTime.of(2026, 6, 17, 12, 0));
        FeedItem second = feedItem(2L, studyId, userId, 102L, LocalDateTime.of(2026, 6, 17, 11, 0));
        FeedItem extra = feedItem(3L, studyId, userId, 103L, LocalDateTime.of(2026, 6, 17, 10, 0));

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(true);
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(true);
        given(feedItemRepository.findByStudyIdWithCursor(studyId, null, null, PageRequest.of(0, size + 1)))
                .willReturn(List.of(first, second, extra));
        given(userRepository.findAllById(List.of(userId)))
                .willReturn(List.of(user(userId, "테스트유저")));

        // when
        FeedCursorResponse result = feedService.getFeedItems(studyId, userId, null, null, size);

        // then
        assertThat(result.items()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor().id()).isEqualTo(2L);
        assertThat(result.nextCursor().occurredAt()).isEqualTo(second.getOccurredAt());
    }

    @Test
    void activeMember가_아니면_피드를_조회할_수_없다() {
        // given
        Long studyId = 1L;
        Long userId = 2L;

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(true);

        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId,
                userId,
                StudyMemberStatus.ACTIVE
        )).willReturn(false);

        // when & then
        assertThatThrownBy(() -> feedService.getFeedItems(studyId, userId, null, null, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.STUDY_ACCESS_DENIED.getMessage());

        then(feedItemRepository).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    void draft_또는_inactiveStudy는_피드를_조회할_수_없다() {
        // given
        Long studyId = 1L;
        Long userId = 2L;

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> feedService.getFeedItems(studyId, userId, null, null, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.STUDY_ACCESS_DENIED.getMessage());

        then(studyMemberRepository).shouldHaveNoInteractions();
        then(feedItemRepository).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    void 커서_시간과_id_중_하나만_있으면_예외() {
        // given
        Long studyId = 1L;
        Long userId = 2L;

        given(studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE)))
                .willReturn(true);
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> feedService.getFeedItems(
                studyId,
                userId,
                LocalDateTime.of(2026, 6, 17, 10, 0),
                null,
                10
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        then(feedItemRepository).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
    }

    private FeedItem feedItem(Long id, Long studyId, Long actorId, Long sourceId, LocalDateTime occurredAt) {
        FeedItem feedItem = FeedItem.createStudyActivity(
                studyId,
                actorId,
                sourceId,
                "스터디 활동 내용",
                occurredAt
        );
        ReflectionTestUtils.setField(feedItem, "id", id);
        return feedItem;
    }

    private User user(Long id, String nickname) {
        User user = User.createUser("user" + id + "@test.com", "password", nickname, "profileImage");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
