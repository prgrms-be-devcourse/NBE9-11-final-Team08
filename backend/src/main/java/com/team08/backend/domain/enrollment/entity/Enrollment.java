package com.team08.backend.domain.enrollment.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "enrollments",
        indexes = @Index(name = "idx_enrollments_user_course", columnList = "user_id, course_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_enrollments_user_course", columnNames = {"user_id", "course_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    private LocalDateTime canceledAt;

    private LocalDateTime expiredAt;

    public static Enrollment active(User user, Course course, Order order, Clock clock) {
        Enrollment enrollment = new Enrollment();
        enrollment.user = user;
        enrollment.course = course;
        enrollment.order = order;
        enrollment.status = EnrollmentStatus.ACTIVE;
        enrollment.enrolledAt = LocalDateTime.now(clock);
        return enrollment;
    }
}
