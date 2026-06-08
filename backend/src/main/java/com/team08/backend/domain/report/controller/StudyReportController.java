package com.team08.backend.domain.report.controller;

import com.team08.backend.domain.report.dto.StudyReportResponse;
import com.team08.backend.domain.report.service.StudyReportService;
import com.team08.backend.global.error.ErrorResponse;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/studies")
@Tag(name = "Study Report", description = "스터디 종료 후 학습 리포트 발급 API")
public class StudyReportController {

    private final StudyReportService studyReportService;

    @Operation(
            summary = "스터디 학습 리포트 발급",
            description = "현재 사용자가 소속된 스터디의 기간이 완료된 경우 총 시청 시간, 댓글 수, 진행률, 일별 히트맵 데이터를 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 스터디 ID 또는 스터디 기간 미완료", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "스터디 구성원이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "스터디 또는 사용자를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{studyId}/report")
    public StudyReportResponse issueReport(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "스터디 ID", example = "1")
            @Positive
            @PathVariable Long studyId
    ) {
        return studyReportService.issueReport(principal.user().id(), studyId);
    }
}
