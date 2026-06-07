package com.team08.backend.domain.attendance.controller;

import com.team08.backend.domain.attendance.entity.AttendanceLog;
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

    @PostMapping
    public ResponseEntity<String> checkIn(@RequestParam Long userId) {
        LocalDate today = LocalDate.now();
        AttendanceLog log = attendanceService.checkIn(userId, today);
        return ResponseEntity.ok("출석이 완료되었습니다! 현재 연속 출석일: " + log.getConsecutiveDays() + "일");
    }
}
