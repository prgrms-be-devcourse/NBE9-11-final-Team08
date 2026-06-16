package com.team08.backend.domain.cart.entity;

import com.team08.backend.domain.cartitem.entity.CartItem;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private Cart(Long userId) {
        this.userId = userId;
    }

    public static Cart create(Long userId) {
        return new Cart(userId);
    }

    public void addItem(Long courseId) {
        boolean alreadyInCart = items.stream()
                .anyMatch(item -> item.getCourseId().equals(courseId));
        if (alreadyInCart) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_IN_CART);
        }

        items.add(CartItem.create(this, courseId));
    }

    public void removeItem(Long cartItemId) {
        boolean removed = items.removeIf(item -> Objects.equals(item.getId(), cartItemId));
        if (!removed) {
            throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    public void clearItems() {
        items.clear();
    }
}
