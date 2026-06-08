package com.team08.backend.domain.attendance.repository;

import com.team08.backend.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 당일 출석 여부 확인
    boolean existsByUserIdAndAttendanceDate(Long userId, LocalDate date);

    // 특정 날짜의 출석 기록 조회
    Optional<Attendance> findByUserIdAndAttendanceDate(Long userId, LocalDate date);

    // 특정 기간 동안의 출석 횟수 조회
    long countByUserIdAndAttendanceDateBetween(Long userId, LocalDate start, LocalDate end);
}
