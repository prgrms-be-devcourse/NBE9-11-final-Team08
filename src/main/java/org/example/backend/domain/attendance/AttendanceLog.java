package org.example.backend.domain.attendance;

import org.example.backend.domain.user.User;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "attendance_logs",
        indexes = @Index(name = "idx_attendance_logs_user_date", columnList = "user_id, attendance_date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_logs_user_date", columnNames = {"user_id", "attendance_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceLog {

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
}
