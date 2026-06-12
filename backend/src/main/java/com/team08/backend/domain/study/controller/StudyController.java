package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/me")
    public List<StudySummaryResponse> getMyStudies(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return studyService.getMyStudies(loginUserPrincipal.user().id());
    }

    @GetMapping("/{studyId}")
    public StudyDetailResponse getStudyDetail(
            @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return studyService.getStudyDetail(studyId, loginUserPrincipal.user().id());
    }

    @GetMapping("/by-course/{courseId}")
    public StudyDetailResponse getStudyDetailByCourseId(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return studyService.getStudyDetailByCourseId(courseId, loginUserPrincipal.user().id());
    }
}
