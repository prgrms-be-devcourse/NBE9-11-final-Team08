package com.team08.backend.domain.lectureprogress.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLectureProgress is a Querydsl query type for LectureProgress
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLectureProgress extends EntityPathBase<LectureProgress> {

    private static final long serialVersionUID = 1810159895L;

    public static final QLectureProgress lectureProgress = new QLectureProgress("lectureProgress");

    public final BooleanPath completed = createBoolean("completed");

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> lastPositionSeconds = createNumber("lastPositionSeconds", Integer.class);

    public final NumberPath<Long> lectureId = createNumber("lectureId", Long.class);

    public final NumberPath<Integer> progressRate = createNumber("progressRate", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Integer> watchedSeconds = createNumber("watchedSeconds", Integer.class);

    public QLectureProgress(String variable) {
        super(LectureProgress.class, forVariable(variable));
    }

    public QLectureProgress(Path<? extends LectureProgress> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLectureProgress(PathMetadata metadata) {
        super(LectureProgress.class, metadata);
    }

}

