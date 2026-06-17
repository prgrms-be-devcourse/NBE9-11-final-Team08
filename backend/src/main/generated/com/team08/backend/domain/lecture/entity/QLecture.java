package com.team08.backend.domain.lecture.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLecture is a Querydsl query type for Lecture
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLecture extends EntityPathBase<Lecture> {

    private static final long serialVersionUID = -2133060457L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLecture lecture = new QLecture("lecture");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final com.team08.backend.domain.chapter.entity.QChapter chapter;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> durationSeconds = createNumber("durationSeconds", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isFreePreview = createBoolean("isFreePreview");

    public final StringPath m3u8Path = createString("m3u8Path");

    public final NumberPath<Integer> orderNo = createNumber("orderNo", Integer.class);

    public final StringPath summary = createString("summary");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QLecture(String variable) {
        this(Lecture.class, forVariable(variable), INITS);
    }

    public QLecture(Path<? extends Lecture> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLecture(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLecture(PathMetadata metadata, PathInits inits) {
        this(Lecture.class, metadata, inits);
    }

    public QLecture(Class<? extends Lecture> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chapter = inits.isInitialized("chapter") ? new com.team08.backend.domain.chapter.entity.QChapter(forProperty("chapter"), inits.get("chapter")) : null;
    }

}

