package com.team08.backend.domain.chapter.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChapter is a Querydsl query type for Chapter
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChapter extends EntityPathBase<Chapter> {

    private static final long serialVersionUID = -1507541833L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChapter chapter = new QChapter("chapter");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final com.team08.backend.domain.course.entity.QCourse course;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<com.team08.backend.domain.lecture.entity.Lecture, com.team08.backend.domain.lecture.entity.QLecture> lectures = this.<com.team08.backend.domain.lecture.entity.Lecture, com.team08.backend.domain.lecture.entity.QLecture>createList("lectures", com.team08.backend.domain.lecture.entity.Lecture.class, com.team08.backend.domain.lecture.entity.QLecture.class, PathInits.DIRECT2);

    public final NumberPath<Integer> orderNo = createNumber("orderNo", Integer.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QChapter(String variable) {
        this(Chapter.class, forVariable(variable), INITS);
    }

    public QChapter(Path<? extends Chapter> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChapter(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChapter(PathMetadata metadata, PathInits inits) {
        this(Chapter.class, metadata, inits);
    }

    public QChapter(Class<? extends Chapter> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.course = inits.isInitialized("course") ? new com.team08.backend.domain.course.entity.QCourse(forProperty("course")) : null;
    }

}

