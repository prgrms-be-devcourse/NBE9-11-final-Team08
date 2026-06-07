package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.study.exception.InvalidStudyApplicationStatusException;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "study_applications",
        indexes = @Index(name = "idx_study_applications_study_user", columnList = "study_id, user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_study_applications_study_user", columnNames = {"study_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime processedAt;

    private StudyApplication(Study study, User user, String message) {
        this.study = study;
        this.user = user;
        this.message = message;
        this.status = ApplicationStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public static StudyApplication create(Study study, User user, String message) {
        return new StudyApplication(study, user, message);
    }

    public void approve() {
        validatePending();

        status = ApplicationStatus.APPROVED;
        processedAt = LocalDateTime.now();
    }

    public void reject() {
        validatePending();

        status = ApplicationStatus.REJECTED;
        processedAt = LocalDateTime.now();
    }

    private void validatePending() {
        if (status != ApplicationStatus.PENDING) {
            throw new InvalidStudyApplicationStatusException();
        }
    }
}
