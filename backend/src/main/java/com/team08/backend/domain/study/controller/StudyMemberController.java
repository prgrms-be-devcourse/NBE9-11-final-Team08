package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.response.StudyMemberResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies/{studyId}/members")
@RequiredArgsConstructor
public class StudyMemberController {

    private final StudyService studyService;

    @GetMapping
    public List<StudyMemberResponse> getStudyMembers(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId
    ) {
        return studyService.getStudyMembers(studyId, principal.user().id());
    }


    @PatchMapping("/{memberId}/kick")
    public ResponseEntity<Void> kickStudyMember(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long memberId
    ) {
        studyService.kickStudyMember(studyId, memberId, principal.user().id());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leaveStudy(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId
    ) {
        studyService.leaveStudy(studyId, principal.user().id());

        return ResponseEntity.noContent().build();
    }
}
