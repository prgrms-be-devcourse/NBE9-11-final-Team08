package com.team08.backend.domain.enrollment.entity;

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

    public void cancel(LocalDateTime canceledAt) {
        this.status = EnrollmentStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    public void expire(LocalDateTime expiredAt) {
        this.status = EnrollmentStatus.EXPIRED;
        this.expiredAt = expiredAt;
    }
}
