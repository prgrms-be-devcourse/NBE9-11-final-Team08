package com.team08.backend.domain.enrollment.dto;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        Long orderId,
        EnrollmentStatus status,
        LocalDateTime enrolledAt
) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getOrder().getId(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt()
        );
    }
}
