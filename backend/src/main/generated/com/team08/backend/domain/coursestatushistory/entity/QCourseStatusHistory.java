package com.team08.backend.domain.coursestatushistory.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCourseStatusHistory is a Querydsl query type for CourseStatusHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCourseStatusHistory extends EntityPathBase<CourseStatusHistory> {

    private static final long serialVersionUID = -850329801L;

    public static final QCourseStatusHistory courseStatusHistory = new QCourseStatusHistory("courseStatusHistory");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final NumberPath<Long> changedBy = createNumber("changedBy", Long.class);

    public final NumberPath<Long> courseId = createNumber("courseId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.team08.backend.domain.course.entity.CourseStatus> fromStatus = createEnum("fromStatus", com.team08.backend.domain.course.entity.CourseStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath reason = createString("reason");

    public final EnumPath<com.team08.backend.domain.course.entity.CourseStatus> toStatus = createEnum("toStatus", com.team08.backend.domain.course.entity.CourseStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCourseStatusHistory(String variable) {
        super(CourseStatusHistory.class, forVariable(variable));
    }

    public QCourseStatusHistory(Path<? extends CourseStatusHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCourseStatusHistory(PathMetadata metadata) {
        super(CourseStatusHistory.class, metadata);
    }

}

