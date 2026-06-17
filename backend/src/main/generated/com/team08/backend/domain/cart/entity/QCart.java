package com.team08.backend.domain.cart.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCart is a Querydsl query type for Cart
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCart extends EntityPathBase<Cart> {

    private static final long serialVersionUID = -979133583L;

    public static final QCart cart = new QCart("cart");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<com.team08.backend.domain.cartitem.entity.CartItem, com.team08.backend.domain.cartitem.entity.QCartItem> items = this.<com.team08.backend.domain.cartitem.entity.CartItem, com.team08.backend.domain.cartitem.entity.QCartItem>createList("items", com.team08.backend.domain.cartitem.entity.CartItem.class, com.team08.backend.domain.cartitem.entity.QCartItem.class, PathInits.DIRECT2);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QCart(String variable) {
        super(Cart.class, forVariable(variable));
    }

    public QCart(Path<? extends Cart> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCart(PathMetadata metadata) {
        super(Cart.class, metadata);
    }

}

