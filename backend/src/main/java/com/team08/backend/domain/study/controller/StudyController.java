package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudyIdResponse;
import com.team08.backend.domain.study.dto.response.StudyMemberResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "스터디", description = "스터디 목록 및 상세 조회 API")
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @Operation(
            summary = "내 스터디 목록 조회",
            description = "현재 로그인한 사용자가 ACTIVE 멤버로 참여 중인 스터디 목록을 조회합니다."
    )
    @GetMapping("/me")
    public List<StudySummaryResponse> getMyStudies(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return studyService.getMyStudies(loginUserPrincipal.user().id());
    }

    @Operation(
            summary = "스터디 상세 조회",
            description = "스터디 ID로 DRAFT가 아닌 스터디의 상세 정보를 조회합니다."
    )
    @GetMapping("/{studyId}")
    public StudyDetailResponse getStudyDetail(
            @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        Long userId = loginUserPrincipal == null ? null : loginUserPrincipal.user().id();

        return studyService.getStudyDetail(studyId, userId);
    }

    @Operation(
            summary = "스터디 멤버 목록 조회",
            description = "스터디에 ACTIVE 상태로 참여 중인 멤버 목록을 조회합니다."
    )
    @GetMapping("/{studyId}/members")
    public List<StudyMemberResponse> getStudyMembers(
            @PathVariable Long studyId
    ) {
        return studyService.getStudyMembers(studyId);
    }

    @Operation(
            summary = "강좌별 스터디 ID 조회",
            description = "강좌 ID에 연결된 DRAFT가 아닌 스터디의 ID를 조회합니다."
    )
    @GetMapping("/by-course/{courseId}")
    public StudyIdResponse getStudyIdByCourseId(@PathVariable Long courseId) {
        return new StudyIdResponse(
                studyService.getStudyIdByCourseId(courseId)
        );
    }
}
