package com.team08.backend.domain.report.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "daily_lecture_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_lecture_stats_user_course_date",
                columnNames = {"user_id", "course_id", "stat_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyLectureStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    private Integer completedLectureCount = 0;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static DailyLectureStat create(User user, Course course, LocalDate statDate) {
        DailyLectureStat stat = new DailyLectureStat();
        stat.user = user;
        stat.course = course;
        stat.statDate = statDate;
        stat.updatedAt = LocalDateTime.now();
        return stat;
    }

    public void increaseCompletedLectureCount() {
        this.completedLectureCount++;
        this.updatedAt = LocalDateTime.now();
    }
}
