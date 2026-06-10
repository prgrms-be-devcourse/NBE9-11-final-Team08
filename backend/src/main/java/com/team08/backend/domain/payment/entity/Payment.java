package com.team08.backend.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long orderId;
    private String paymentKey;
    private String method;
    @Column(nullable = false)
    private Integer amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private String failedReason;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
