package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.YearMonth;
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

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    @DisplayName("일반적인 출석 시 연속 출석일과 누적 출석일이 1씩 증가한다")
    void checkIn_normalDay_increasesAttendanceCount() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        Attendance yesterdayLog = Attendance.record(
                userId,
                yesterday,
                0,
                0);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.of(yesterdayLog));
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(1L);

        // saveAndFlush()가 호출되면 저장된 객체(인자)를 그대로 반환하도록 설정
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(i -> i.getArguments()[0]);

        // when
        Attendance todayLog = attendanceService.checkIn(userId, today);

        // then
        assertEquals(2, todayLog.getConsecutiveDays());
        assertEquals(2, todayLog.getMonthlyTotalDays());
        verify(attendanceRepository, times(1)).saveAndFlush(any(Attendance.class));
    }

    @Test
    @DisplayName("당일 중복 출석 시 예외가 발생한다")
    void checkIn_duplicate_throwsException() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.empty());
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(0L);

        when(attendanceRepository.saveAndFlush(any(Attendance.class)))
                .thenThrow(new DataIntegrityViolationException("DB Unique Constraint Violation"));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            attendanceService.checkIn(userId, today);
        });
        assertEquals(ErrorCode.ATTENDANCE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("결석 후 출석 시 연속 출석일이 1로 초기화된다")
    void checkIn_resetConsecutiveDays() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.empty());
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(3L);

        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(i -> i.getArguments()[0]);

        // when
        Attendance todayLog = attendanceService.checkIn(userId, today);

        // then
        assertEquals(1, todayLog.getConsecutiveDays());
        assertEquals(4, todayLog.getMonthlyTotalDays());
        verify(attendanceRepository, times(1)).saveAndFlush(any(Attendance.class));
    }
}
