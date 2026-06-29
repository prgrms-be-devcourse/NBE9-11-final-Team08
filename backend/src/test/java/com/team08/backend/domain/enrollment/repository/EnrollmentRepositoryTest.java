package com.team08.backend.domain.enrollment.repository;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void courseIdLookupReturnsOnlyActiveEnrollments() {
        Long userId = 1L;
        Long courseId = 100L;
        LocalDateTime now = LocalDateTime.of(2026, 6, 30, 10, 0);
        Order refundedOrder = saveOrder(userId, "ORD-REFUNDED", now.minusDays(1));
        Enrollment canceledEnrollment = Enrollment.createActive(userId, courseId, refundedOrder, now.minusDays(1));
        canceledEnrollment.cancel(now);
        enrollmentRepository.saveAndFlush(canceledEnrollment);

        List<Long> beforeRepurchase = enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                userId,
                List.of(courseId)
        );

        assertThat(beforeRepurchase).isEmpty();

        Order newOrder = saveOrder(userId, "ORD-REPURCHASE", now.plusMinutes(1));
        enrollmentRepository.saveAndFlush(Enrollment.createActive(userId, courseId, newOrder, now.plusMinutes(1)));

        List<Long> afterRepurchase = enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                userId,
                List.of(courseId)
        );

        assertThat(afterRepurchase).containsExactly(courseId);
        assertThat(enrollmentRepository.findAll()).extracting(Enrollment::getStatus)
                .containsExactlyInAnyOrder(EnrollmentStatus.CANCELED, EnrollmentStatus.ACTIVE);
    }

    private Order saveOrder(Long userId, String orderNumber, LocalDateTime orderedAt) {
        return orderRepository.saveAndFlush(Order.createPendingPayment(userId, orderNumber, orderedAt));
    }
}
