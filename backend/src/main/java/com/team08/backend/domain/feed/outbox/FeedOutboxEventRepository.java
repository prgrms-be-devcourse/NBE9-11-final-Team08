package com.team08.backend.domain.feed.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedOutboxEventRepository extends JpaRepository<FeedOutboxEvent, Long> {

    List<FeedOutboxEvent> findByStatusInOrderByIdAsc(List<FeedOutboxEventStatus> statuses, Pageable pageable);

    List<FeedOutboxEvent> findByStudyIdAndStatusAndIdGreaterThanOrderByIdAsc(
            Long studyId,
            FeedOutboxEventStatus status,
            Long id
    );
}
