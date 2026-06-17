package com.team08.backend.domain.learningevent.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLearningEvent is a Querydsl query type for LearningEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningEvent extends EntityPathBase<LearningEvent> {

    private static final long serialVersionUID = 1081459063L;

    public static final QLearningEvent learningEvent = new QLearningEvent("learningEvent");

    public final NumberPath<Long> chapterId = createNumber("chapterId", Long.class);

    public final NumberPath<Long> courseId = createNumber("courseId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> eventTime = createDateTime("eventTime", java.time.LocalDateTime.class);

    public final EnumPath<LearningEventType> eventType = createEnum("eventType", LearningEventType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> lectureId = createNumber("lectureId", Long.class);

    public final NumberPath<Integer> positionSeconds = createNumber("positionSeconds", Integer.class);

    public final StringPath uniqueEventKey = createString("uniqueEventKey");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QLearningEvent(String variable) {
        super(LearningEvent.class, forVariable(variable));
    }

    public QLearningEvent(Path<? extends LearningEvent> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLearningEvent(PathMetadata metadata) {
        super(LearningEvent.class, metadata);
    }

}

