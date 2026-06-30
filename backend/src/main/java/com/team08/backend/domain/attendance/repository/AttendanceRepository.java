package com.team08.backend.domain.attendance.repository;

import com.team08.backend.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // 특정 날짜의 출석 기록 조회
    Optional<Attendance> findByUserIdAndAttendanceDate(Long userId, LocalDate date);

    // 특정 기간 동안의 출석 횟수 조회
    long countByUserIdAndAttendanceDateBetween(Long userId, LocalDate start, LocalDate end);

    // 특정 기간 동안의 출석 기록 목록 조회
    List<Attendance> findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(Long userId, LocalDate start, LocalDate end);

    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u WHERE NOT EXISTS (SELECT 1 FROM Attendance a WHERE a.userId = u.id AND a.attendanceDate >= :thresholdDate) AND EXISTS (SELECT 1 FROM Attendance a WHERE a.userId = u.id AND a.attendanceDate >= :limitDate)")
    List<Long> findInactiveUserIds(@org.springframework.data.repository.query.Param("thresholdDate") LocalDate thresholdDate, @org.springframework.data.repository.query.Param("limitDate") LocalDate limitDate);
}
