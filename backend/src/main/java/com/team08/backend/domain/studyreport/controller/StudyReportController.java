package com.team08.backend.domain.studyreport.controller;

import com.team08.backend.domain.studyreport.dto.StudyReportResponse;
import com.team08.backend.domain.studyreport.service.StudyReportService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "학습 리포트", description = "스터디 학습 리포트 생성 및 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/report")
public class StudyReportController {

    private final StudyReportService studyReportService;

    @Operation(summary = "학습 리포트 생성/갱신", description = "학습 이벤트를 집계하여 리포트를 생성합니다. 기존 리포트가 있으면 덮어씁니다.")
    @PostMapping
    public StudyReportResponse generateReport(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return studyReportService.generateReport(principal.user().id(), studyId);
    }

    @Operation(summary = "학습 리포트 조회", description = "저장된 학습 리포트를 조회합니다.")
    @GetMapping
    public StudyReportResponse getReport(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return studyReportService.getReport(principal.user().id(), studyId);
    }
}
