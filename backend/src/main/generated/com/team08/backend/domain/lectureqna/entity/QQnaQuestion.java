package com.team08.backend.domain.lectureqna.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QQnaQuestion is a Querydsl query type for QnaQuestion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQnaQuestion extends EntityPathBase<QnaQuestion> {

    private static final long serialVersionUID = 550434099L;

    public static final QQnaQuestion qnaQuestion = new QQnaQuestion("qnaQuestion");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> lectureId = createNumber("lectureId", Long.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QQnaQuestion(String variable) {
        super(QnaQuestion.class, forVariable(variable));
    }

    public QQnaQuestion(Path<? extends QnaQuestion> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQnaQuestion(PathMetadata metadata) {
        super(QnaQuestion.class, metadata);
    }

}

