package com.team08.backend.domain.studyactivity.controller;

import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.service.StudyActivityService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내 스터디 활동", description = "현재 로그인 사용자의 스터디 활동 조회 API")
@RestController
@RequestMapping("/api/study-activities")
@RequiredArgsConstructor
public class MyStudyActivityController {

    private final StudyActivityService studyActivityService;

    @Operation(
            summary = "내 스터디 활동 목록 조회",
            description = "현재 로그인한 사용자가 작성한 미삭제 스터디 활동을 최신순으로 조회합니다."
    )
    @GetMapping("/me")
    public Page<StudyActivityResponse> getMyActivities(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return studyActivityService.getMyActivities(
                loginUserPrincipal.user().id(),
                pageable
        );
    }
}
