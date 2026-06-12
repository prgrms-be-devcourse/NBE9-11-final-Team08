package com.team08.backend.domain.studyactivity.controller;

import com.team08.backend.domain.studyactivity.dto.StudyActivityCreateRequest;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.service.StudyActivityService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studies/{studyId}/activities")
@RequiredArgsConstructor
public class StudyActivityController {

    private final StudyActivityService studyActivityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyActivityResponse createActivity(
            @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody StudyActivityCreateRequest request
    ) {
        return studyActivityService.createActivity(
                studyId,
                loginUserPrincipal.user().id(),
                request.content()
        );
    }

    @GetMapping
    public Page<StudyActivityResponse> getActivities(
            @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return studyActivityService.getActivities(
                studyId,
                loginUserPrincipal.user().id(),
                pageable
        );
    }

    @GetMapping("/{activityId}")
    public StudyActivityResponse getActivity(
            @PathVariable Long studyId,
            @PathVariable Long activityId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        return studyActivityService.getActivity(
                studyId,
                activityId,
                loginUserPrincipal.user().id()
        );
    }
}
