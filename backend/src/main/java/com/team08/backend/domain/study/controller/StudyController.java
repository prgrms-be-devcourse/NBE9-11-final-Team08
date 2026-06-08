package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.request.StudyCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyRecruitmentStatusUpdateRequest;
import com.team08.backend.domain.study.dto.request.StudyUpdateRequest;
import com.team08.backend.domain.study.dto.request.StudyVisibilityUpdateRequest;
import com.team08.backend.domain.study.dto.response.StudyCreateResponse;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @PostMapping
    public StudyCreateResponse createStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @RequestBody StudyCreateRequest request
    ) {
        Long studyId = studyService.create(principal.user().id(), request);
        return new StudyCreateResponse(studyId);
    }

    @GetMapping
    public List<StudySummaryResponse> getStudies(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        Long userId = principal == null
                ? null
                : principal.user().id();

        return studyService.getStudies(userId);
    }

    @GetMapping("/{id}")
    public StudyDetailResponse getStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id
    ) {
        Long userId = principal == null
                ? null
                : principal.user().id();

        return studyService.findStudy(id, userId);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody StudyUpdateRequest request
    ) {
        studyService.updateStudyInfo(id, principal.user().id(), request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> updateStudyVisibility(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody StudyVisibilityUpdateRequest request
    ) {
        studyService.updateStudyVisibility(id, principal.user().id(), request.visibility());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/recruitment")
    public ResponseEntity<Void> updateStudyRecruitment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody StudyRecruitmentStatusUpdateRequest request
    ) {
        studyService.updateStudyRecruitment(id, principal.user().id(), request.recruitmentStatus());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id
    ) {
        studyService.startStudy(id, principal.user().id());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/end")
    public ResponseEntity<Void> endStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id
    ) {
        studyService.endStudy(id, principal.user().id());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id
    ) {
        studyService.deleteStudy(id, principal.user().id());

        return ResponseEntity.noContent().build();
    }
}
