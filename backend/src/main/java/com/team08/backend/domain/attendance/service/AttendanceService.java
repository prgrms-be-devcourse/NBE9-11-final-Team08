package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.entity.AttendanceLog;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Transactional
    public AttendanceLog checkIn(Long userId, LocalDate today) {

        // 오늘 이미 출석했는지 검증 (중복 출석 방지)
        if ((boolean) attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)) {
            throw new IllegalStateException("오늘은 이미 출석하셨습니다.");
        }

        //  어제 출석 기록 조회
        LocalDate yesterday = today.minusDays(1);
        Optional<AttendanceLog> yesterdayAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday);

        // 연속 출석일 계산
        int consecutiveDays = yesterdayAttendance
                .map(log -> log.getConsecutiveDays() + 1)
                .orElse(1);

        User user = User.builder().id(userId).build();

        AttendanceLog todayLog = AttendanceLog.builder()
                .user(user)
                .attendanceDate(today)
                .consecutiveDays(consecutiveDays)
                .monthlyTotalDays(1)
                .build();

        attendanceRepository.save(todayLog);

        return todayLog;
    }
}
