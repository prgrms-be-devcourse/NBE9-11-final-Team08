package org.example.backend.domain.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELED,
    REFUNDED,
    EXPIRED
}
