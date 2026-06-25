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

    @Operation(
            summary = "학습 리포트 조회/갱신 (read-or-upsert)",
            description = """
                    학습 리포트를 조회한다. 리포트가 없으면 즉시 집계해 생성한다.
                    응답의 status 로 결과를 구분한다.
                    - LOADED: 기존 리포트를 그대로 조회 (refresh=false)
                    - REGENERATED: 리포트가 없거나 쿨다운이 지나 새로 집계함
                    - COOLDOWN: refresh=true 였지만 쿨다운 이내라 재집계하지 않고 기존 리포트 반환 (nextRegenerableAt 참고)
                    """)
    @GetMapping
    public StudyReportResponse getReport(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "true 면 재집계를 시도한다(쿨다운 이내면 거부됨). 기본값 false") @RequestParam(defaultValue = "false") boolean refresh,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return studyReportService.getReport(principal.user().id(), studyId, refresh);
    }
}
