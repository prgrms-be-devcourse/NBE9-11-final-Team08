package com.team08.backend.domain.feed.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFeedItem is a Querydsl query type for FeedItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFeedItem extends EntityPathBase<FeedItem> {

    private static final long serialVersionUID = -1502735328L;

    public static final QFeedItem feedItem = new QFeedItem("feedItem");

    public final NumberPath<Long> actorId = createNumber("actorId", Long.class);

    public final StringPath content = createString("content");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> occurredAt = createDateTime("occurredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> sourceId = createNumber("sourceId", Long.class);

    public final NumberPath<Long> studyId = createNumber("studyId", Long.class);

    public final EnumPath<FeedItemType> type = createEnum("type", FeedItemType.class);

    public QFeedItem(String variable) {
        super(FeedItem.class, forVariable(variable));
    }

    public QFeedItem(Path<? extends FeedItem> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFeedItem(PathMetadata metadata) {
        super(FeedItem.class, metadata);
    }

}

