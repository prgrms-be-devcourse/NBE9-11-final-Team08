package com.team08.backend.domain.attendance;

import com.team08.backend.domain.attendance.service.AttendanceService;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AttendanceIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("동일한 날짜에 중복 출석 시 CONFLICT 예외가 발생한다 (DB 제약 조건 검증)")
    void checkIn_duplicateDay_throwsConflictException() {
        // given
        User user = saveUser("attendance_test@example.com");
        LocalDate today = LocalDate.now();

        // 첫 번째 출석 성공
        attendanceService.checkIn(user.getId(), today);

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            // 두 번째 출석 시도 (중복)
            attendanceService.checkIn(user.getId(), today);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("오늘은 이미 출석하셨습니다.");
    }

    private User saveUser(String email) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "test_user");
        ReflectionTestUtils.setField(user, "role", "USER");
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return userRepository.save(user);
    }

    private <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test entity.", e);
        }
    }
}
