package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.request.StudyApplicationCreateRequest;
import com.team08.backend.domain.study.dto.response.StudyApplicationResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/studies/{studyId}/applications")
@RequiredArgsConstructor
public class StudyApplicationController {

    private final StudyService studyService;

    @PostMapping
    public StudyApplicationResponse applyStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @RequestBody StudyApplicationCreateRequest request
    ) {
        return studyService.applyStudy(studyId, principal.user().id(), request);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> cancelStudyApplication(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId
    ) {
        studyService.cancelStudyApplication(studyId, principal.user().id());

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<StudyApplicationResponse> getStudyApplications(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId
    ) {
        return studyService.getStudyApplications(studyId, principal.user().id());
    }

    @PatchMapping("/{applicationId}/approve")
    public ResponseEntity<Void> approveStudyApplication(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long applicationId
    ) {
        studyService.approveStudyApplication(studyId, applicationId, principal.user().id());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{applicationId}/reject")
    public ResponseEntity<Void> rejectStudyApplication(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long applicationId
    ) {
        studyService.rejectStudyApplication(studyId, applicationId, principal.user().id());

        return ResponseEntity.noContent().build();
    }
}
