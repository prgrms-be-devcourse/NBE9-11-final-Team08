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

    // Course 정보가 이후 변경되더라도 주문 내역은 주문 당시 값을 보존해야 하므로 스냅샷 필드로 저장합니다.
    @Column(nullable = false)
    private Long courseId;
    @Column(nullable = false)
    private String courseTitle;
    private String courseThumbnail;
    @Column(nullable = false)
    private Integer price;
    @Column(nullable = false)
    private Integer discountPrice = 0;
    @Column(nullable = false)
    private Integer finalPrice;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private OrderItem(
            Order order,
            Long courseId,
            String courseTitle,
            String courseThumbnail,
            int price,
            int discountPrice,
            int finalPrice,
            LocalDateTime createdAt
    ) {
        this.order = order;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.courseThumbnail = courseThumbnail;
        this.price = price;
        this.discountPrice = discountPrice;
        this.finalPrice = finalPrice;
        this.createdAt = createdAt;
    }

    public static OrderItem createSnapshot(
            Order order,
            Long courseId,
            String courseTitle,
            String courseThumbnail,
            int price,
            int discountPrice,
            int finalPrice,
            LocalDateTime createdAt
    ) {
        return new OrderItem(order, courseId, courseTitle, courseThumbnail, price, discountPrice, finalPrice, createdAt);
    }
}
