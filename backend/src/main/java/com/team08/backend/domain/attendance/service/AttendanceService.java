package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.dto.AttendanceResponse;
import com.team08.backend.domain.attendance.dto.AttendanceStatusResponse;
import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.exception.AttendanceAlreadyExistsException;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.couponreward.outbox.service.CouponRewardOutboxService;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final CouponRewardOutboxService couponRewardOutboxService;
    private final Clock clock;

    // 출석체크
    @Transactional
    public AttendanceResponse checkIn(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);

        // 사용자 검증
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // USER_001
        }

        // 어제 출석 기록 조회
        Optional<Attendance> yesterdayAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday);

        // 이번 달 누적 출석일 조회 (오늘 기록 제외)
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);
        long monthCountBeforeToday = attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, yesterday);

        // 오늘 자 출석 기록 생성
        Attendance todayLog = Attendance.record(
                userId,
                today,
                yesterdayAttendance,
                (int) monthCountBeforeToday,
                now);

        // 출석 기록 저장 (동시성 중복 저장 방어)
        try {
            Attendance savedLog = attendanceRepository.saveAndFlush(todayLog);
            couponRewardOutboxService.createAttendanceCheckedEvent(
                    userId,
                    savedLog.getAttendanceDate(),
                    savedLog.getConsecutiveDays(),
                    savedLog.getMonthlyTotalDays()
            );

            return AttendanceResponse.from(savedLog);
        } catch (DataIntegrityViolationException e) {
            throw new AttendanceAlreadyExistsException();
        }
    }

    // 출석 기록 조회
    @Transactional(readOnly = true)
    public AttendanceStatusResponse getMyAttendance(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);
        LocalDate endOfMonth = YearMonth.from(today).atEndOfMonth();

        // 사용자 검증
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // USER_001
        }

        // 이번 달 출석 기록 조회
        List<Attendance> monthlyAttendances =
                attendanceRepository.findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        userId,
                        startOfMonth,
                        endOfMonth
                );

        Optional<Attendance> todayAttendance = monthlyAttendances.stream()
                .filter(attendance -> attendance.getAttendanceDate().isEqual(today))
                .findFirst();

        Attendance latestAttendance = monthlyAttendances.isEmpty()
                ? null
                : monthlyAttendances.get(monthlyAttendances.size() - 1);

        int consecutiveDays = todayAttendance
                .or(() -> monthlyAttendances.stream()
                        .filter(attendance -> attendance.getAttendanceDate().isEqual(today.minusDays(1)))
                        .findFirst())
                .map(Attendance::getConsecutiveDays)
                .orElse(0);

        int monthlyTotalDays = latestAttendance == null
                ? 0
                : latestAttendance.getMonthlyTotalDays();

        List<Integer> checkedDays = monthlyAttendances.stream()
                .map(attendance -> attendance.getAttendanceDate().getDayOfMonth())
                .toList();

        return new AttendanceStatusResponse(
                todayAttendance.isPresent(),
                consecutiveDays,
                monthlyTotalDays,
                checkedDays
        );
    }
}
