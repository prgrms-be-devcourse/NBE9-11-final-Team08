package com.team08.backend.domain.enrollment.entity;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long courseId;
    @Column(nullable = false)
    private Long orderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EnrollmentStatus status;
    @Column(nullable = false)
    private LocalDateTime enrolledAt;
    private LocalDateTime canceledAt;
    private LocalDateTime expiredAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Enrollment createActive(Long userId, Long courseId, Long orderId, LocalDateTime enrolledAt) {
        return new Enrollment(
                null,
                userId,
                courseId,
                orderId,
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
