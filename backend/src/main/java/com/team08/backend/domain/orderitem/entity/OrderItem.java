package com.team08.backend.domain.orderitem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long orderId;
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

    public static OrderItem createSnapshot(Long orderId, Long courseId, String courseTitle, int price, int discountPrice, int finalPrice, LocalDateTime createdAt) {
        return new OrderItem(
                null,
                orderId,
                courseId,
                courseTitle,
                price,
                discountPrice,
                finalPrice,
                createdAt,
                null
        );
    }
}
