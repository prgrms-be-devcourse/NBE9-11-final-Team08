package com.team08.backend.domain.lectureqna.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QQnaAnswer is a Querydsl query type for QnaAnswer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQnaAnswer extends EntityPathBase<QnaAnswer> {

    private static final long serialVersionUID = -190912437L;

    public static final QQnaAnswer qnaAnswer = new QQnaAnswer("qnaAnswer");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> instructorId = createNumber("instructorId", Long.class);

    public final NumberPath<Long> questionId = createNumber("questionId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QQnaAnswer(String variable) {
        super(QnaAnswer.class, forVariable(variable));
    }

    public QQnaAnswer(Path<? extends QnaAnswer> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQnaAnswer(PathMetadata metadata) {
        super(QnaAnswer.class, metadata);
    }

}

