package com.team08.backend.domain.issuedcoupon.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QIssuedCoupon is a Querydsl query type for IssuedCoupon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QIssuedCoupon extends EntityPathBase<IssuedCoupon> {

    private static final long serialVersionUID = 2130065139L;

    public static final QIssuedCoupon issuedCoupon = new QIssuedCoupon("issuedCoupon");

    public final DateTimePath<java.time.LocalDateTime> expiredAt = createDateTime("expiredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> issuedAt = createDateTime("issuedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> policyId = createNumber("policyId", Long.class);

    public final EnumPath<CouponStatus> status = createEnum("status", CouponStatus.class);

    public final DateTimePath<java.time.LocalDateTime> usedAt = createDateTime("usedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QIssuedCoupon(String variable) {
        super(IssuedCoupon.class, forVariable(variable));
    }

    public QIssuedCoupon(Path<? extends IssuedCoupon> path) {
        super(path.getType(), path.getMetadata());
    }

    public QIssuedCoupon(PathMetadata metadata) {
        super(IssuedCoupon.class, metadata);
    }

}

