package com.team08.backend.domain.attendance.repository;

import com.team08.backend.domain.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {

    boolean existsByUserIdAndAttendanceDate(Long userId, LocalDate date);

    Optional<AttendanceLog> findByUserIdAndAttendanceDate(Long userId, LocalDate date);

    long countByUserIdAndAttendanceDateBetween(Long userId, LocalDate start, LocalDate end);
}
