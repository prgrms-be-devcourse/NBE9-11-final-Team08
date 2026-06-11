package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    // [사용자] 출석체크
    @Transactional
    public Attendance checkIn(Long userId, LocalDate today) {

        // 사용자 검증
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // USER_001
        }

        // 동시성 중복 출석 방어
        if (attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)) {
            throw new CustomException(ErrorCode.ATTENDANCE_ALREADY_EXISTS); // ATTENDANCE_001
        }

        // 어제 출석 기록 조회
        LocalDate yesterday = today.minusDays(1);
        Optional<Attendance> yesterdayAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday);

        // 연속 출석일 계산
        int consecutiveDays = yesterdayAttendance
                .map(log -> log.getConsecutiveDays() + 1)
                .orElse(1);

        // 이번 달 누적 출석일 계산
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);
        long currentMonthCount = attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today);
        int monthlyTotalDays = (int) currentMonthCount + 1;

        // 출석 기록 세팅
        Attendance todayLog = Attendance.create(
                userId,
                today,
                consecutiveDays,
                monthlyTotalDays);

        // 출석 기록 저장
        return attendanceRepository.save(todayLog);
    }
}
