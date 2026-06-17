package com.team08.backend.domain.ordercouponusage.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QOrderCouponUsage is a Querydsl query type for OrderCouponUsage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderCouponUsage extends EntityPathBase<OrderCouponUsage> {

    private static final long serialVersionUID = 269240523L;

    public static final QOrderCouponUsage orderCouponUsage = new QOrderCouponUsage("orderCouponUsage");

    public final NumberPath<Integer> discountAmount = createNumber("discountAmount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> issuedCouponId = createNumber("issuedCouponId", Long.class);

    public final NumberPath<Long> orderId = createNumber("orderId", Long.class);

    public QOrderCouponUsage(String variable) {
        super(OrderCouponUsage.class, forVariable(variable));
    }

    public QOrderCouponUsage(Path<? extends OrderCouponUsage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrderCouponUsage(PathMetadata metadata) {
        super(OrderCouponUsage.class, metadata);
    }

}

