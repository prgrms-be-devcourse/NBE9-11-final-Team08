package com.team08.backend.domain.attendance.controller;

import com.team08.backend.domain.attendance.dto.AttendanceResponse;
import com.team08.backend.domain.attendance.entity.Attendance;
import com.team08.backend.domain.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Tag(name = "출석 API", description = "사용자 출석 체크 및 조회 관련 API")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // [사용자] 출석 체크 /api/attendances
    @PostMapping
    @Operation(summary = "출석 체크", description = "당일 출석을 수행하고 연속/누적 출석일수를 반환합니다.")
    public AttendanceResponse checkIn(
            @Parameter(description = "출석을 수행할 사용자의 ID", example = "1")
            @RequestParam Long userId) {
        LocalDate today = LocalDate.now();

        // [사용자] 출석체크
        Attendance log = attendanceService.checkIn(userId, today);
        return AttendanceResponse.from(log);
    }
}
