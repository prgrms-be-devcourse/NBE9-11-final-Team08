package com.team08.backend.domain.aifeedback.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAiFeedback is a Querydsl query type for AiFeedback
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAiFeedback extends EntityPathBase<AiFeedback> {

    private static final long serialVersionUID = -1669525589L;

    public static final QAiFeedback aiFeedback = new QAiFeedback("aiFeedback");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final StringPath activityContentSnapshot = createString("activityContentSnapshot");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath feedback = createString("feedback");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath modelName = createString("modelName");

    public final StringPath promptVersion = createString("promptVersion");

    public final EnumPath<AiFeedbackStatus> status = createEnum("status", AiFeedbackStatus.class);

    public final NumberPath<Long> studyActivityId = createNumber("studyActivityId", Long.class);

    public final NumberPath<Long> studyId = createNumber("studyId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAiFeedback(String variable) {
        super(AiFeedback.class, forVariable(variable));
    }

    public QAiFeedback(Path<? extends AiFeedback> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAiFeedback(PathMetadata metadata) {
        super(AiFeedback.class, metadata);
    }

}

