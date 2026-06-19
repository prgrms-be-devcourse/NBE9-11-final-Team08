package com.team08.backend.domain.feed.service;

import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedItemRepository feedItemRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    public Page<FeedItemResponse> getPagedFeedItems(Long studyId, Long userId, Pageable pageable) {
        // TODO: 수강권도 확인이 필요하고 스터디 쪽도 마찬가지, 추후 StudyAccessValidator 와 같은 통합 검증으로 변경 예정
        validateActiveStudy(studyId);
        validateActiveStudyMember(studyId, userId);

        Pageable latestFirstPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("occurredAt"),
                        Sort.Order.desc("id")
                )
        );

        return feedItemRepository
                .findAllByStudyId(studyId, latestFirstPageable)
                .map(FeedItemResponse::from);
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
}
