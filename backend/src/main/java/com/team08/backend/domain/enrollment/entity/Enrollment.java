package com.team08.backend.domain.enrollment.entity;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}),
        indexes = {
                @Index(name = "idx_enrollment_user_status_course", columnList = "user_id, status, course_id"),
                @Index(name = "idx_enrollment_order_status", columnList = "order_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long courseId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EnrollmentStatus status;
    @Column(nullable = false)
    private LocalDateTime enrolledAt;
    private LocalDateTime canceledAt;
    private LocalDateTime expiredAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Enrollment(
            Long id,
            Long userId,
            Long courseId,
            Order order,
            EnrollmentStatus status,
            LocalDateTime enrolledAt,
            LocalDateTime canceledAt,
            LocalDateTime expiredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.order = order;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.canceledAt = canceledAt;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Enrollment createActive(Long userId, Long courseId, Order order, LocalDateTime enrolledAt) {
        return new Enrollment(
                null,
                userId,
                courseId,
                order,
                EnrollmentStatus.ACTIVE,
                enrolledAt,
                null,
                null,
                enrolledAt,
                null
        );
    }

    public void cancel(LocalDateTime canceledAt) {
        validateStatus(EnrollmentStatus.ACTIVE);
        this.status = EnrollmentStatus.CANCELED;
        this.canceledAt = canceledAt;
        this.updatedAt = canceledAt;
    }

    public void expire(LocalDateTime expiredAt) {
        validateStatus(EnrollmentStatus.ACTIVE);
        this.status = EnrollmentStatus.EXPIRED;
        this.expiredAt = expiredAt;
        this.updatedAt = expiredAt;
    }

    private void validateStatus(EnrollmentStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new CustomException(ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION);
        }
    }
}
