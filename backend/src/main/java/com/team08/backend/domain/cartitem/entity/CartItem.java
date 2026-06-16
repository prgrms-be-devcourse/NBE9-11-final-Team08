package com.team08.backend.domain.cartitem.entity;

import com.team08.backend.domain.cart.entity.Cart;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "course_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private Long courseId;

    private CartItem(Cart cart, Long courseId) {
        this.cart = cart;
        this.courseId = courseId;
    }

    public static CartItem create(Cart cart, Long courseId) {
        return new CartItem(cart, courseId);
    }
}
