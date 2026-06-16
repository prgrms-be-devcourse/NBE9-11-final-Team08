package com.team08.backend.domain.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "attendances", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "attendance_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private Integer consecutiveDays;

    @Column(nullable = false)
    private Integer monthlyTotalDays;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Attendance(Long userId, LocalDate attendanceDate, Integer consecutiveDays, Integer monthlyTotalDays, LocalDateTime createdAt) {
        this.userId = userId;
        this.attendanceDate = attendanceDate;
        this.consecutiveDays = consecutiveDays;
        this.monthlyTotalDays = monthlyTotalDays;
        this.createdAt = createdAt;
    }

    // 오늘 자 출석 기록 생성
    public static Attendance record(Long userId, LocalDate today, Optional<Attendance> yesterdayAttendance, int monthCountBeforeToday, LocalDateTime now) {
        int consecutive = yesterdayAttendance
                .map(Attendance::getConsecutiveDays)
                .orElse(0) + 1;

        int monthly = monthCountBeforeToday + 1;

        return new Attendance(userId, today, consecutive, monthly, now);
    }
}
