package com.team08.backend.domain.attendance.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "attendances",
        indexes = @Index(name = "idx_attendances_user_date", columnList = "user_id, attendance_date"), // 인덱스 이름도 함께 변경
        uniqueConstraints = @UniqueConstraint(name = "uk_attendances_user_date", columnNames = {"user_id", "attendance_date"}) // 제약조건 이름도 함께 변경
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private Integer consecutiveDays;

    @Column(nullable = false)
    private Integer monthlyTotalDays;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 테스트 코드에서 쓰는 중
    @Builder
    public Attendance(User user, LocalDate attendanceDate, Integer consecutiveDays, Integer monthlyTotalDays) {
        this.user = user;
        this.attendanceDate = attendanceDate;
        this.consecutiveDays = consecutiveDays;
        this.monthlyTotalDays = monthlyTotalDays;
        this.createdAt = LocalDateTime.now();
    }
}
