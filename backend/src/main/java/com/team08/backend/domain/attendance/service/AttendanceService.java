package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.coupon.service.IssuedCouponService;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final IssuedCouponService issuedCouponService;
    private final UserRepository userRepository;

    // [사용자] 출석체크 및 보상 지급
    @Transactional
    public Attendance checkIn(Long userId, LocalDate today) {

        // 당일 출석 여부 확인
        if (attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘은 이미 출석하셨습니다.");
        }

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));

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
        Attendance todayLog = Attendance.builder()
                .user(user)
                .attendanceDate(today)
                .consecutiveDays(consecutiveDays)
                .monthlyTotalDays(monthlyTotalDays)
                .build();

        // 출석 기록 저장 (동시성 중복 출석 방어)
        try {
            attendanceRepository.save(todayLog);
            attendanceRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘은 이미 출석하셨습니다.");
        }

        if (consecutiveDays == 7) {
            // [시스템] 출석 보상 쿠폰 자동 발급
            issuedCouponService.issueAttendanceCoupon(userId);
        }

        return todayLog;
    }
}
