package com.team08.backend.domain.order.entity;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void sameCourseCannotBeAddedTwice() {
        LocalDateTime orderedAt = LocalDateTime.parse("2026-06-17T12:00:00");
        Order order = Order.createPendingPayment(1L, "ORD-20260617120000-ABC12345", orderedAt);

        order.addItem(1000L, "Spring", 30_000, orderedAt);

        assertThatThrownBy(() -> order.addItem(1000L, "Spring", 30_000, orderedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ITEM_ALREADY_EXISTS));
    }
}
