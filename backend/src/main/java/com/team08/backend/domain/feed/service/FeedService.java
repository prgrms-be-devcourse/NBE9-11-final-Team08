package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feed.dto.response.FeedCursor;
import com.team08.backend.domain.feed.dto.response.FeedCursorResponse;
import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedItemRepository feedItemRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;

    public FeedCursorResponse getFeedItems(
            Long studyId,
            Long userId,
            LocalDateTime cursorOccurredAt,
            Long cursorId,
            int size
    ) {
        // TODO: 수강권도 확인이 필요하고 스터디 쪽도 마찬가지, 추후 StudyAccessValidator 와 같은 통합 검증으로 변경 예정
        validateFeedAccess(studyId, userId);
        validateCursor(cursorOccurredAt, cursorId);

        int normalizedSize = normalizeSize(size);
        List<FeedItem> feedItems = feedItemRepository.findByStudyIdWithCursor(
                studyId,
                cursorOccurredAt,
                cursorId,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = feedItems.size() > normalizedSize;
        List<FeedItem> pageItems = hasNext
                ? feedItems.subList(0, normalizedSize)
                : feedItems;

        FeedCursor nextCursor = resolveNextCursor(pageItems, hasNext);
        Map<Long, String> actorNicknames = findActorNicknames(pageItems);
        List<FeedItemResponse> responses = pageItems.stream()
                .map(feedItem -> FeedItemResponse.from(
                        feedItem,
                        actorNicknames.getOrDefault(feedItem.getActorId(), "알 수 없는 사용자")
                ))
                .toList();

        return new FeedCursorResponse(responses, nextCursor, hasNext);
    }

    public void validateFeedAccess(Long studyId, Long userId) {
        validateActiveStudy(studyId);
        validateActiveStudyMember(studyId, userId);
    }

    private void validateActiveStudy(Long studyId) {
        if (!studyRepository.existsByIdAndStatusNotIn(studyId, List.of(StudyStatus.DRAFT, StudyStatus.INACTIVE))) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private void validateActiveStudyMember(Long studyId, Long userId) {
        if ((!studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private void validateCursor(LocalDateTime cursorOccurredAt, Long cursorId) {
        if ((cursorOccurredAt == null) != (cursorId == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }

        return Math.min(size, 50);
    }

    private FeedCursor resolveNextCursor(List<FeedItem> pageItems, boolean hasNext) {
        if (!hasNext || pageItems.isEmpty()) {
            return null;
        }

        FeedItem lastItem = pageItems.get(pageItems.size() - 1);
        return new FeedCursor(lastItem.getOccurredAt(), lastItem.getId());
    }

    private Map<Long, String> findActorNicknames(List<FeedItem> feedItems) {
        List<Long> actorIds = feedItems.stream()
                .map(FeedItem::getActorId)
                .distinct()
                .toList();

        return userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        User::getNickname,
                        (left, right) -> left
                ));
    }
}
