package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "lecture_progresses",
        indexes = {
                @Index(name = "idx_lecture_progress_user_updated", columnList = "user_id, updated_at"),
                @Index(name = "idx_lecture_progress_user_lecture", columnList = "user_id, lecture_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_lecture_progress_user_lecture", columnNames = {"user_id", "lecture_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer lastPositionSeconds = 0;

    @Column(nullable = false)
    private Integer watchedSeconds = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static LectureProgress start(Lecture lecture, User user) {
        LocalDateTime now = LocalDateTime.now();
        LectureProgress progress = new LectureProgress();
        progress.lecture = lecture;
        progress.user = user;
        progress.createdAt = now;
        progress.updatedAt = now;
        return progress;
    }

    public boolean updatePosition(int positionSeconds) {
        int durationSeconds = lecture.getDurationSeconds();
        int clampedPosition = Math.max(0, Math.min(positionSeconds, durationSeconds));
        boolean wasCompleted = completed;

        this.lastPositionSeconds = clampedPosition;
        this.watchedSeconds = Math.max(watchedSeconds, clampedPosition);

        if (durationSeconds > 0 && clampedPosition >= Math.ceil(durationSeconds * 0.95)) {
            complete();
        }

        this.updatedAt = LocalDateTime.now();
        return !wasCompleted && completed;
    }

    public boolean complete() {
        if (completed) {
            return false;
        }

        this.completed = true;
        this.completedAt = LocalDateTime.now();
        this.lastPositionSeconds = lecture.getDurationSeconds();
        this.watchedSeconds = lecture.getDurationSeconds();
        this.updatedAt = completedAt;
        return true;
    }

    public int getProgressPercent() {
        if (completed) {
            return 100;
        }
        if (lecture.getDurationSeconds() == null || lecture.getDurationSeconds() == 0) {
            return 0;
        }
        return (int) Math.floor((double) lastPositionSeconds / lecture.getDurationSeconds() * 100);
    }
}
