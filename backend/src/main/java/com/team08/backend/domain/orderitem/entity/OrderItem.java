package com.team08.backend.domain.orderitem.entity;

import com.team08.backend.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Course can change later, so order items store the purchase-time snapshot values.
    @Column(nullable = false)
    private Long courseId;
    @Column(nullable = false)
    private String courseTitle;
    @Column(nullable = false)
    private Integer price;
    @Column(nullable = false)
    private Integer discountPrice = 0;
    @Column(nullable = false)
    private Integer finalPrice;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private OrderItem(Order order, Long courseId, String courseTitle, int price, int discountPrice, int finalPrice, LocalDateTime createdAt) {
        this.order = order;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.price = price;
        this.discountPrice = discountPrice;
        this.finalPrice = finalPrice;
        this.createdAt = createdAt;
    }

    public static OrderItem createSnapshot(Order order, Long courseId, String courseTitle, int price, int discountPrice, int finalPrice, LocalDateTime createdAt) {
        return new OrderItem(order, courseId, courseTitle, price, discountPrice, finalPrice, createdAt);
    }
}
