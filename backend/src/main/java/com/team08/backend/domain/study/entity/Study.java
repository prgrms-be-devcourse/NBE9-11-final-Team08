package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.study.exception.*;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "studies")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 255, nullable = false)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyRecruitmentStatus recruitmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyVisibility visibility;

    private Integer maxMemberCount;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Study(User owner, String title, String description,StudyVisibility visibility,
                  LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.status = StudyStatus.READY;
        this.recruitmentStatus = StudyRecruitmentStatus.RECRUITING;
        this.visibility = visibility;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Study create(User owner, String title, String description, StudyVisibility visibility,
                               LocalDate plannedStartDate, LocalDate plannedEndDate) {

        validateTitle(title);
        validatePeriod(plannedStartDate, plannedEndDate);

        return new Study(
                owner,
                title,
                description,
                visibility,
                plannedStartDate,
                plannedEndDate
        );
    }

    public void validateOwner(Long userId) {
        if (!isOwner(userId)) {
            throw new StudyAccessDeniedException();
        }
    }

    public void delete() {
        deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() { return deletedAt != null; }

    public void updateInfo(String title, String description, LocalDate plannedStartDate, LocalDate plannedEndDate) {
        validateEditable();

        if (!(title != null && title.isBlank())) {
            this.title = title;
        }

        if (!(description != null && description.isBlank())) {
            this.description = description;
        }

        LocalDate newPlannedStartDate = plannedStartDate != null
                ? plannedStartDate : this.plannedStartDate;

        LocalDate newPlannedEndDate = plannedEndDate != null
                ? plannedEndDate : this.plannedEndDate;

        validatePeriod(newPlannedStartDate, newPlannedEndDate);

        this.plannedStartDate = newPlannedStartDate;
        this.plannedEndDate = newPlannedEndDate;
    }

    public void changeVisibility(StudyVisibility visibility) {
        validateEditable();

        this.visibility = visibility;
    }

    public void changeRecruitmentStatus(StudyRecruitmentStatus recruitmentStatus) {
        validateEditable();

        this.recruitmentStatus = recruitmentStatus;
    }

    public void startStudy(LocalDate startedAt) {
        if (status != StudyStatus.READY) {
            throw new InvalidStudyStatusTransitionException();
        }

        actualStartDate = startedAt;
        status = StudyStatus.IN_PROGRESS;
    }

    public void endStudy(LocalDate endedAt) {
        if (status != StudyStatus.IN_PROGRESS) {
            throw new InvalidStudyStatusTransitionException();
        }

        actualEndDate = endedAt;
        status = StudyStatus.CLOSED;
    }

    public LocalDate getStartDate() {
        return status == StudyStatus.READY ? plannedStartDate : actualStartDate;
    }

    public LocalDate getEndDate() {
        return status == StudyStatus.CLOSED ? actualEndDate : plannedEndDate;
    }

    public void validateCanReceiveApplicationFrom(Long userId) {
        if (isRecruitingClosed()) {
            throw new StudyRecruitmentClosedException();
        }

        if (isOwner(userId)) {
            throw new StudyAlreadyMemberException();
        }
    }

    public boolean isOwner(Long userId) {
        return owner.getId().equals(userId);
    }

    private boolean isRecruitingClosed() {
        return recruitmentStatus == StudyRecruitmentStatus.CLOSED;
    }

    private static void validateTitle(String title) {
        if (title.isBlank()) {
            throw new InvalidStudyTitleException();
        }
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null &&
            endDate != null &&
            endDate.isBefore(startDate)) {

            throw new InvalidStudyPeriodException();
        }
    }

    private void validateEditable() {
        if (!status.isEditable()) {
            throw new StudyNotEditableException();
        }
    }
}
