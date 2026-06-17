package com.team08.backend.domain.lecturereflection.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLectureReflection is a Querydsl query type for LectureReflection
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLectureReflection extends EntityPathBase<LectureReflection> {

    private static final long serialVersionUID = 634417879L;

    public static final QLectureReflection lectureReflection = new QLectureReflection("lectureReflection");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> lectureId = createNumber("lectureId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QLectureReflection(String variable) {
        super(LectureReflection.class, forVariable(variable));
    }

    public QLectureReflection(Path<? extends LectureReflection> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLectureReflection(PathMetadata metadata) {
        super(LectureReflection.class, metadata);
    }

}

