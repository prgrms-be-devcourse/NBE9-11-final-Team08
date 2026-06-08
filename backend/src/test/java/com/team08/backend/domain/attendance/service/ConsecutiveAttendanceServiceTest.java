package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.coupon.service.IssuedCouponService;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsecutiveAttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private IssuedCouponService issuedCouponService;

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
                .consecutiveDays(1)
                .build();

        when(userRepository.getReferenceById(userId)).thenReturn(mock(User.class));
        when(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).thenReturn(false);
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

        // 4. 7일 연속이 아니므로 쿠폰 발급 로직은 절대 실행되지 않아야 함! (중요)
        verify(issuedCouponService, never()).issueAttendanceCoupon(anyLong());
    }

    @Test
    @DisplayName("당일 중복 출석 시 예외가 발생한다")
    void checkIn_duplicate_throwsException() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        // 이미 출석했다고 설정
        when(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).thenReturn(true);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.checkIn(userId, today);
        });
        assertEquals("오늘은 이미 출석하셨습니다.", exception.getMessage());

        // 중복 출석 시 쿠폰 로직은 실행되면 안 됨
        verify(issuedCouponService, never()).issueAttendanceCoupon(anyLong());
    }


    @Test
    @DisplayName("7일 연속 출석 시 이벤트 쿠폰이 정상적으로 발급된다")
    void checkIn_7thConsecutiveDay_issuesCoupon() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        // 어제까지 6일 연속 출석했다고 가정
        Attendance yesterdayLog = Attendance.builder()
                .consecutiveDays(6)
                .build();

        when(userRepository.getReferenceById(userId)).thenReturn(mock(User.class));
        when(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).thenReturn(false);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.of(yesterdayLog));
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(6L);

        // when
        Attendance todayLog = attendanceService.checkIn(userId, today);

        // then
        assertEquals(7, todayLog.getConsecutiveDays());
        assertEquals(7, todayLog.getMonthlyTotalDays());

        verify(attendanceRepository, times(1)).save(any(Attendance.class));

        // 7일 연속이므로 이벤트 쿠폰 발급 로직이 1번 호출되었는지 확인!
        verify(issuedCouponService, times(1)).issueAttendanceCoupon(userId);
    }

    @Test
    @DisplayName("8일 연속 출석 시에는 이벤트 쿠폰이 발급되지 않는다 (7일차에만 발급)")
    void checkIn_8thConsecutiveDay_doesNotIssueCoupon() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        // 어제까지 7일 연속 출석했다고 가정 (오늘은 8일 차)
        Attendance yesterdayLog = Attendance.builder()
                .consecutiveDays(7)
                .build();

        when(userRepository.getReferenceById(userId)).thenReturn(mock(User.class));
        when(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).thenReturn(false);
        when(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)).thenReturn(Optional.of(yesterdayLog));
        when(attendanceRepository.countByUserIdAndAttendanceDateBetween(userId, startOfMonth, today)).thenReturn(7L);

        // when
        Attendance todayLog = attendanceService.checkIn(userId, today);

        // then
        assertEquals(8, todayLog.getConsecutiveDays()); // 8일 차 출석 성공

        // 7일 차가 아니므로 쿠폰 발급 로직은 실행되지 않아야 함!
        verify(issuedCouponService, never()).issueAttendanceCoupon(anyLong());
    }

    @Test
    @DisplayName("결석 후 출석 시 연속 출석일이 1로 초기화된다")
    void checkIn_resetConsecutiveDays() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfMonth = YearMonth.from(today).atDay(1);

        when(userRepository.getReferenceById(userId)).thenReturn(mock(User.class));
        when(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).thenReturn(false);
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
        verify(issuedCouponService, never()).issueAttendanceCoupon(anyLong()); // 쿠폰 발급 안 됨
    }
}
