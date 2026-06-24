package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.dto.AttendanceResponse;
import com.team08.backend.domain.attendance.dto.AttendanceStatusResponse;
import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.exception.AttendanceAlreadyExistsException;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssuedCouponService issuedCouponService;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.systemDefault());

    @InjectMocks
    private AttendanceService attendanceService;

    private LocalDateTime now;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
        today = now.toLocalDate();
    }

    @Test
    @DisplayName("일반적인 출석 시 연속 출석일과 누적 출석일이 1씩 증가한다")
    void checkIn_normalDay_increasesAttendanceCount() {
        // given
        Long userId = 1L;
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        Attendance yesterdayLog = Attendance.record(
                userId,
                yesterday,
                Optional.empty(), // simplify for creation
                0,
                now.minusDays(1));
        
        // manually set consecutive days to 1 for the test
        ReflectionTestUtils.setField(yesterdayLog, "consecutiveDays", 1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.of(yesterdayLog));
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, yesterday)).thenReturn(1L);

        // saveAndFlush()가 호출되면 저장된 객체(인자)를 그대로 반환하도록 설정
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(i -> i.getArguments()[0]);

        // when
        AttendanceResponse response = attendanceService.checkIn(userId);

        // then
        assertEquals(2, response.consecutiveDays());
        assertEquals(2, response.monthlyTotalDays());
        verify(attendanceRepository, times(1)).saveAndFlush(any(Attendance.class));
    }

    @Test
    @DisplayName("당일 중복 출석 시 예외가 발생한다")
    void checkIn_duplicate_throwsException() {
        // given
        Long userId = 1L;
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.empty());
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, yesterday)).thenReturn(0L);

        when(attendanceRepository.saveAndFlush(any(Attendance.class)))
                .thenThrow(new DataIntegrityViolationException("DB Unique Constraint Violation"));

        // when & then
        AttendanceAlreadyExistsException exception = assertThrows(AttendanceAlreadyExistsException.class, () -> {
            attendanceService.checkIn(userId);
        });
        assertEquals(ErrorCode.ATTENDANCE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("결석 후 출석 시 연속 출석일이 1로 초기화된다")
    void checkIn_resetConsecutiveDays() {
        // given
        Long userId = 1L;
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.empty());
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, yesterday)).thenReturn(3L);

        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(i -> i.getArguments()[0]);

        // when
        AttendanceResponse response = attendanceService.checkIn(userId);

        // then
        assertEquals(1, response.consecutiveDays());
        assertEquals(4, response.monthlyTotalDays());
        verify(attendanceRepository, times(1)).saveAndFlush(any(Attendance.class));
    }

    @Test
    @DisplayName("7일 연속 출석 시 보상 쿠폰이 자동 발급된다")
    void checkIn_consecutive7Days_issuesCoupon() {
        // given
        Long userId = 1L;
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        // 어제 자 기록의 연속 출석일수가 6일이어야 오늘이 7일째가 됨
        Attendance yesterdayLog = Attendance.record(userId, yesterday, Optional.empty(), 0, now.minusDays(1));
        ReflectionTestUtils.setField(yesterdayLog, "consecutiveDays", 6);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.of(yesterdayLog));
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, yesterday)).thenReturn(10L);
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(i -> i.getArguments()[0]);

        // when
        AttendanceResponse response = attendanceService.checkIn(userId);

        // then
        assertEquals(7, response.consecutiveDays());
        verify(issuedCouponService, times(1)).issueAttendanceCoupon(userId);
    }

    @Test
    @DisplayName("이번 달 출석 현황을 조회한다")
    void getMyAttendance_returnsMonthlyStatus() {
        // given
        Long userId = 1L;
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);
        LocalDate endOfMonth = YearMonth.from(today).atEndOfMonth();

        Attendance firstLog = Attendance.record(userId, today.minusDays(2), Optional.empty(), 0, now.minusDays(2));
        Attendance yesterdayLog = Attendance.record(userId, today.minusDays(1), Optional.of(firstLog), 1, now.minusDays(1));
        Attendance todayLog = Attendance.record(userId, today, Optional.of(yesterdayLog), 2, now);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                userId,
                startOfMonth,
                endOfMonth
        )).thenReturn(List.of(firstLog, yesterdayLog, todayLog));

        // when
        AttendanceStatusResponse response = attendanceService.getMyAttendance(userId);

        // then
        assertEquals(true, response.checkedToday());
        assertEquals(3, response.consecutiveDays());
        assertEquals(3, response.monthlyTotalDays());
        assertEquals(List.of(today.minusDays(2).getDayOfMonth(), today.minusDays(1).getDayOfMonth(), today.getDayOfMonth()), response.checkedDays());
    }

    @Test
    @DisplayName("오늘 출석하지 않았으면 어제까지의 현황을 조회한다")
    void getMyAttendance_notCheckedToday_returnsPreviousStatus() {
        // given
        Long userId = 1L;
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);
        LocalDate endOfMonth = YearMonth.from(today).atEndOfMonth();

        Attendance firstLog = Attendance.record(userId, today.minusDays(2), Optional.empty(), 0, now.minusDays(2));
        Attendance yesterdayLog = Attendance.record(userId, today.minusDays(1), Optional.of(firstLog), 1, now.minusDays(1));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                userId,
                startOfMonth,
                endOfMonth
        )).thenReturn(List.of(firstLog, yesterdayLog));

        // when
        AttendanceStatusResponse response = attendanceService.getMyAttendance(userId);

        // then
        assertEquals(false, response.checkedToday());
        assertEquals(2, response.consecutiveDays());
        assertEquals(2, response.monthlyTotalDays());
        assertEquals(List.of(today.minusDays(2).getDayOfMonth(), today.minusDays(1).getDayOfMonth()), response.checkedDays());
    }
}
