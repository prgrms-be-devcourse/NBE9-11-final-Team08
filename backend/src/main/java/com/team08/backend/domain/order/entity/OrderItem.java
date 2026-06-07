package com.team08.backend.domain.order.entity;

import com.team08.backend.domain.course.entity.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

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

    public static OrderItem create(Order order, Course course, Integer discountPrice, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);

        OrderItem orderItem = new OrderItem();
        orderItem.order = order;
        orderItem.course = course;
        orderItem.courseTitle = course.getTitle();
        orderItem.price = course.getPrice();
        orderItem.discountPrice = discountPrice;
        orderItem.finalPrice = course.getPrice() - discountPrice;
        orderItem.createdAt = now;
        return orderItem;
    }
}
