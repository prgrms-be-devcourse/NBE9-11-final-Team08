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
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedItemRepository feedItemRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    public FeedCursorResponse getFeedItems(
            Long studyId,
            Long userId,
            LocalDateTime cursorOccurredAt,
            Long cursorId,
            int size
    ) {
        // TODO: 수강권도 확인이 필요하고 스터디 쪽도 마찬가지, 추후 StudyAccessValidator 와 같은 통합 검증으로 변경 예정
        validateActiveStudy(studyId);
        validateActiveStudyMember(studyId, userId);
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
        List<FeedItemResponse> responses = pageItems.stream()
                .map(FeedItemResponse::from)
                .toList();

        return new FeedCursorResponse(responses, nextCursor, hasNext);
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
}
