package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.dto.AttendanceResponse;
import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    public AttendanceResponse checkIn(Long userId, LocalDate today) {

        // 사용자 검증
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // USER_001
        }

        // 어제 출석 기록 조회
        LocalDate yesterday = today.minusDays(1);
        Optional<Attendance> yesterdayAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday);

        // 이번 달 누적 출석일 조회
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);
        long currentMonthCount = attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today);

        // 연속 출석일 계산
        int lastConsecutive = yesterdayAttendance
                .map(Attendance::getConsecutiveDays)
                .orElse(0);

        // 오늘 자 출석 기록 생성
        Attendance todayLog = Attendance.record(userId, today, lastConsecutive, (int) currentMonthCount);

        // 출석 기록 저장 (동시성 중복 저장 방어)
        try {
            Attendance savedLog = attendanceRepository.saveAndFlush(todayLog);
            return AttendanceResponse.from(savedLog);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ATTENDANCE_ALREADY_EXISTS);
        }
    }
}
