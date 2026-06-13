package com.team08.backend.domain.cartitem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "course_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long cartId;
    @Column(nullable = false)
    private Long courseId;
}
