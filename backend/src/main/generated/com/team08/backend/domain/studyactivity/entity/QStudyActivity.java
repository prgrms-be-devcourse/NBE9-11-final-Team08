package com.team08.backend.domain.studyactivity.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStudyActivity is a Querydsl query type for StudyActivity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyActivity extends EntityPathBase<StudyActivity> {

    private static final long serialVersionUID = -1574834377L;

    public static final QStudyActivity studyActivity = new QStudyActivity("studyActivity");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final NumberPath<Long> authorId = createNumber("authorId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> studyId = createNumber("studyId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QStudyActivity(String variable) {
        super(StudyActivity.class, forVariable(variable));
    }

    public QStudyActivity(Path<? extends StudyActivity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStudyActivity(PathMetadata metadata) {
        super(StudyActivity.class, metadata);
    }

}

