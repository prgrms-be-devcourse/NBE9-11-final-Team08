package com.team08.backend.domain.attendance.repository;

import com.team08.backend.domain.attendance.entity.AttendanceLog;

import java.time.LocalDate;
import java.util.Optional;

public class AttendanceRepository {
    public Object existsByUserIdAndAttendanceDate(Long userId, LocalDate today) {
        return null;
    }

    public Optional<AttendanceLog> findByUserIdAndAttendanceDate(Long userId, LocalDate yesterday) {
        return null;
    }

    public void save(Object any) {
        
    }
}
