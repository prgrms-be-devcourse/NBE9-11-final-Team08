package com.team08.backend.domain.feed.repository;

import com.team08.backend.domain.feed.entity.FeedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {
}
