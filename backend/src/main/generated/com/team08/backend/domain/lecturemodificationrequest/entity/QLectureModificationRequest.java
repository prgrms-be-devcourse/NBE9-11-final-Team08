package com.team08.backend.domain.lecturemodificationrequest.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLectureModificationRequest is a Querydsl query type for LectureModificationRequest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLectureModificationRequest extends EntityPathBase<LectureModificationRequest> {

    private static final long serialVersionUID = -1516878757L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLectureModificationRequest lectureModificationRequest = new QLectureModificationRequest("lectureModificationRequest");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final StringPath afterM3u8Path = createString("afterM3u8Path");

    public final StringPath beforeM3u8Path = createString("beforeM3u8Path");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> instructorId = createNumber("instructorId", Long.class);

    public final com.team08.backend.domain.lecture.entity.QLecture lecture;

    public final NumberPath<Long> managedBy = createNumber("managedBy", Long.class);

    public final StringPath rejectedReason = createString("rejectedReason");

    public final EnumPath<RequestStatus> status = createEnum("status", RequestStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QLectureModificationRequest(String variable) {
        this(LectureModificationRequest.class, forVariable(variable), INITS);
    }

    public QLectureModificationRequest(Path<? extends LectureModificationRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLectureModificationRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLectureModificationRequest(PathMetadata metadata, PathInits inits) {
        this(LectureModificationRequest.class, metadata, inits);
    }

    public QLectureModificationRequest(Class<? extends LectureModificationRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.lecture = inits.isInitialized("lecture") ? new com.team08.backend.domain.lecture.entity.QLecture(forProperty("lecture"), inits.get("lecture")) : null;
    }

}

