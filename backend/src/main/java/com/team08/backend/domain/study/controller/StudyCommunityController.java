package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.request.StudyCommentCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyCommentUpdateRequest;
import com.team08.backend.domain.study.dto.request.StudyPostCreateRequest;
import com.team08.backend.domain.study.dto.request.StudyPostUpdateRequest;
import com.team08.backend.domain.study.dto.response.StudyCommentResponse;
import com.team08.backend.domain.study.dto.response.StudyPostDetailResponse;
import com.team08.backend.domain.study.dto.response.StudyPostSummaryResponse;
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
@RequestMapping("/api/studies/{studyId}/posts")
@RequiredArgsConstructor
public class StudyCommunityController {

    private final StudyService studyService;

    @PostMapping
    public StudyPostDetailResponse createStudyPost(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @RequestBody StudyPostCreateRequest request
    ) {
        return studyService.createStudyPost(studyId, principal.user().id(), request);
    }

    @GetMapping
    public List<StudyPostSummaryResponse> getStudyPosts(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId
    ) {
        return studyService.getStudyPosts(studyId, principal.user().id());
    }

    @GetMapping("/{postId}")
    public StudyPostDetailResponse getStudyPost(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long postId
    ) {
        return studyService.getStudyPost(studyId, postId, principal.user().id());
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<Void> updateStudyPost(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long postId,
            @RequestBody StudyPostUpdateRequest request
    ) {
        studyService.updateStudyPost(studyId, postId, principal.user().id(), request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deleteStudyPost(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long postId
    ) {
        studyService.deleteStudyPost(studyId, postId, principal.user().id());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comments")
    public StudyCommentResponse createStudyComment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long postId,
            @RequestBody StudyCommentCreateRequest request
    ) {
        return studyService.createStudyComment(studyId, postId, principal.user().id(), request);
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> updateStudyComment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody StudyCommentUpdateRequest request
    ) {
        studyService.updateStudyComment(studyId, postId, commentId, principal.user().id(), request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteStudyComment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long studyId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        studyService.deleteStudyComment(studyId, postId, commentId, principal.user().id());

        return ResponseEntity.noContent().build();
    }
}
