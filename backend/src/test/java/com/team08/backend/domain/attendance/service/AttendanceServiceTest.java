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
import static org.mockito.Mockito.*;

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

        // 어제 1일 차 출석을 완료했다고 가정
        Attendance yesterdayLog = Attendance.builder()
                .userId(userId)
                .attendanceDate(yesterday)
                .consecutiveDays(1)
                .monthlyTotalDays(1)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.of(yesterdayLog));
        // 이번 달 누적 출석 수가 1번이었다고 가정
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(1L);

        // when
        Attendance todayLog = attendanceService.checkIn(userId, today);

        // then
        // 1. 연속 출석일이 2일로 계산되었는지 확인
        assertEquals(2, todayLog.getConsecutiveDays());
        // 2. 누적 출석일이 2일로 계산되었는지 확인
        assertEquals(2, todayLog.getMonthlyTotalDays());

        // 3. DB에 잘 저장되었는지 확인
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("당일 중복 출석 시 예외가 발생한다")
    void checkIn_duplicate_throwsException() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        when(userRepository.existsById(userId)).thenReturn(true);
        // DB 제약 조건 위반 발생 모의
        doThrow(DataIntegrityViolationException.class).when(attendanceRepository).save(any(Attendance.class));

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
        // 어제 출석 기록이 없음 (결석)
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.empty());
        // 이번 달 기존 누적 출석 수는 3번이었다고 가정
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(3L);

        // when
        Attendance todayLog = attendanceService.checkIn(userId, today);

        // then
        assertEquals(1, todayLog.getConsecutiveDays()); // 연속 출석일은 1로 초기화됨
        assertEquals(4, todayLog.getMonthlyTotalDays()); // 누적 출석일은 기존 3 + 1 = 4로 정상 증가

        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }
}
