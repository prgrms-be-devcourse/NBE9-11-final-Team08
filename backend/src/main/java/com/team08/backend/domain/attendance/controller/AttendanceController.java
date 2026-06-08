package com.team08.backend.domain.attendance.controller;

import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // [사용자] 출석 체크 /api/v1/attendances
    @PostMapping
    public ResponseEntity<String> checkIn(@RequestParam Long userId) {
        LocalDate today = LocalDate.now();
        // [사용자] 출석체크 및 보상 지급
        Attendance log = attendanceService.checkIn(userId, today);
        return ResponseEntity.ok("출석이 완료되었습니다! 현재 연속 출석일: " + log.getConsecutiveDays() + "일");
    }
}
