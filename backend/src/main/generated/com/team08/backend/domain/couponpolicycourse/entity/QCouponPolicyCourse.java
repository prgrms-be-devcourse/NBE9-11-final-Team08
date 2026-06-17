package com.team08.backend.domain.couponpolicycourse.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCouponPolicyCourse is a Querydsl query type for CouponPolicyCourse
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCouponPolicyCourse extends EntityPathBase<CouponPolicyCourse> {

    private static final long serialVersionUID = -4612905L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCouponPolicyCourse couponPolicyCourse = new QCouponPolicyCourse("couponPolicyCourse");

    public final com.team08.backend.domain.couponpolicy.entity.QCouponPolicy couponPolicy;

    public final NumberPath<Long> courseId = createNumber("courseId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QCouponPolicyCourse(String variable) {
        this(CouponPolicyCourse.class, forVariable(variable), INITS);
    }

    public QCouponPolicyCourse(Path<? extends CouponPolicyCourse> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCouponPolicyCourse(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCouponPolicyCourse(PathMetadata metadata, PathInits inits) {
        this(CouponPolicyCourse.class, metadata, inits);
    }

    public QCouponPolicyCourse(Class<? extends CouponPolicyCourse> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.couponPolicy = inits.isInitialized("couponPolicy") ? new com.team08.backend.domain.couponpolicy.entity.QCouponPolicy(forProperty("couponPolicy")) : null;
    }

}

