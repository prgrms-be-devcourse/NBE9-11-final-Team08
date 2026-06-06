package com.team08.backend.domain.attendance.service;

import com.team08.backend.domain.attendance.entity.AttendanceLog;
import com.team08.backend.domain.attendance.repository.AttendanceRepository;
import com.team08.backend.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks
    private AttendanceService attendanceService;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Test
    @DisplayName("성공: 어제 출석한 유저가 오늘 출석하면 연속 출석일이 1 증가한다.")
    void checkIn_Success_Consecutive() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 6, 6);
        LocalDate yesterday = today.minusDays(1);

        // 1. 테스트용 User 객체 생성 (User 엔티티에 맞게 생성, 임시로 id만 세팅한다고 가정)
        // 만약 User에 @Builder가 있다면 User.builder().id(userId).build() 사용
        User mockUser = org.mockito.Mockito.mock(User.class);

        // 오늘 출석 안 함
        given(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).willReturn(false);

        // 2. 여기서 에러났던 부분을 AttendanceLog 엔티티 구조에 맞게 수정!
        AttendanceLog yesterdayAttendance = AttendanceLog.builder()
                .user(mockUser)
                .attendanceDate(yesterday)
                .consecutiveDays(3) // 어제까지 연속 3일 출석했다고 가정
                .monthlyTotalDays(5) // 이번달 누적 5일이라고 가정
                .build();

        given(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday))
                .willReturn(Optional.of(yesterdayAttendance));

        // When
        AttendanceLog todayAttendance = attendanceService.checkIn(userId, today);

        // Then
        assertThat(todayAttendance.getConsecutiveDays()).isEqualTo(4); // 3 + 1 = 4
        verify(attendanceRepository).save(any());
    }

    @Test
    @DisplayName("성공: 어제 결석한 유저가 오늘 출석하면 연속 출석일이 1로 초기화된다.")
    void checkIn_Success_ResetConsecutive() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 6, 6);
        LocalDate yesterday = today.minusDays(1);

        given(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).willReturn(false);
        // 어제 기록 없음 (결석)
        given(attendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday))
                .willReturn(Optional.empty());

        // When
        AttendanceLog todayAttendance = attendanceService.checkIn(userId, today);

        // Then
        assertThat(todayAttendance.getConsecutiveDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("실패: 오늘 이미 출석한 유저가 다시 요청하면 예외가 발생한다.")
    void checkIn_Fail_AlreadyAttended() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 6, 6);

        // 오늘 이미 출석함
        given(attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> attendanceService.checkIn(userId, today))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("오늘은 이미 출석하셨습니다.");
    }
}
