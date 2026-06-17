package com.team08.backend.domain.course.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCourse is a Querydsl query type for Course
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCourse extends EntityPathBase<Course> {

    private static final long serialVersionUID = -1921999641L;

    public static final QCourse course = new QCourse("course");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final NumberPath<Long> categoryId = createNumber("categoryId", Long.class);

    public final ListPath<com.team08.backend.domain.chapter.entity.Chapter, com.team08.backend.domain.chapter.entity.QChapter> chapters = this.<com.team08.backend.domain.chapter.entity.Chapter, com.team08.backend.domain.chapter.entity.QChapter>createList("chapters", com.team08.backend.domain.chapter.entity.Chapter.class, com.team08.backend.domain.chapter.entity.QChapter.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> instructorId = createNumber("instructorId", Long.class);

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final EnumPath<CourseStatus> status = createEnum("status", CourseStatus.class);

    public final StringPath thumbnail = createString("thumbnail");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public QCourse(String variable) {
        super(Course.class, forVariable(variable));
    }

    public QCourse(Path<? extends Course> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCourse(PathMetadata metadata) {
        super(Course.class, metadata);
    }

}

