package com.team08.backend.domain.order.entity;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, unique = true)
    private String orderNumber;
    @Column(nullable = false)
    private Integer totalPrice;
    @Column(nullable = false)
    private Integer discountPrice = 0;
    @Column(nullable = false)
    private Integer finalPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OrderStatus status;
    @Column(nullable = false)
    private LocalDateTime orderedAt;
    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;
    private LocalDateTime expiredAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Order createPendingPayment(Long userId, String orderNumber, int totalPrice, int discountPrice, int finalPrice, LocalDateTime orderedAt) {
        return new Order(
                null,
                userId,
                orderNumber,
                totalPrice,
                discountPrice,
                finalPrice,
                OrderStatus.PENDING_PAYMENT,
                orderedAt,
                null,
                null,
                null,
                null,
                orderedAt,
                null
        );
    }

    public void markPaid(LocalDateTime paidAt) {
        validateStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = paidAt;
    }

    public void cancel(LocalDateTime canceledAt) {
        validateStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.CANCELED;
        this.canceledAt = canceledAt;
        this.updatedAt = canceledAt;
    }

    public void refund(LocalDateTime refundedAt) {
        validateStatus(OrderStatus.PAID);
        this.status = OrderStatus.REFUNDED;
        this.refundedAt = refundedAt;
        this.updatedAt = refundedAt;
    }

    public void expire(LocalDateTime expiredAt) {
        validateStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.EXPIRED;
        this.expiredAt = expiredAt;
        this.updatedAt = expiredAt;
    }

    private void validateStatus(OrderStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }
}
