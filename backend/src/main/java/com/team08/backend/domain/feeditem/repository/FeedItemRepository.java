package com.team08.backend.domain.feeditem.repository;

import com.team08.backend.domain.feeditem.entity.FeedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {
}
