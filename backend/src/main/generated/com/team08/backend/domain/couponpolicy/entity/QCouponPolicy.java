package com.team08.backend.domain.couponpolicy.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCouponPolicy is a Querydsl query type for CouponPolicy
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCouponPolicy extends EntityPathBase<CouponPolicy> {

    private static final long serialVersionUID = -88923647L;

    public static final QCouponPolicy couponPolicy = new QCouponPolicy("couponPolicy");

    public final com.team08.backend.global.common.QBaseTimeEntity _super = new com.team08.backend.global.common.QBaseTimeEntity(this);

    public final NumberPath<Long> categoryId = createNumber("categoryId", Long.class);

    public final EnumPath<CouponTarget> couponTarget = createEnum("couponTarget", CouponTarget.class);

    public final EnumPath<CouponType> couponType = createEnum("couponType", CouponType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final EnumPath<DiscountType> discountType = createEnum("discountType", DiscountType.class);

    public final NumberPath<Integer> discountValue = createNumber("discountValue", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isStackable = createBoolean("isStackable");

    public final DateTimePath<java.time.LocalDateTime> issueEndDate = createDateTime("issueEndDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> issueStartDate = createDateTime("issueStartDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> maxDiscountAmount = createNumber("maxDiscountAmount", Integer.class);

    public final NumberPath<Integer> minOrderAmount = createNumber("minOrderAmount", Integer.class);

    public final StringPath name = createString("name");

    public final ListPath<com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse, com.team08.backend.domain.couponpolicycourse.entity.QCouponPolicyCourse> targetCourses = this.<com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse, com.team08.backend.domain.couponpolicycourse.entity.QCouponPolicyCourse>createList("targetCourses", com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse.class, com.team08.backend.domain.couponpolicycourse.entity.QCouponPolicyCourse.class, PathInits.DIRECT2);

    public final NumberPath<Integer> totalQuantity = createNumber("totalQuantity", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<CouponUsageType> usageType = createEnum("usageType", CouponUsageType.class);

    public final NumberPath<Integer> validDays = createNumber("validDays", Integer.class);

    public QCouponPolicy(String variable) {
        super(CouponPolicy.class, forVariable(variable));
    }

    public QCouponPolicy(Path<? extends CouponPolicy> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCouponPolicy(PathMetadata metadata) {
        super(CouponPolicy.class, metadata);
    }

}

