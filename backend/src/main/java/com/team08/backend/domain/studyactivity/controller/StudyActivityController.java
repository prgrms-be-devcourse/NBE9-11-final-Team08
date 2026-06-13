package com.team08.backend.domain.studyactivity.controller;

import com.team08.backend.domain.studyactivity.dto.StudyActivityCreateRequest;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.dto.StudyActivityUpdateRequest;
import com.team08.backend.domain.studyactivity.service.StudyActivityService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "스터디 활동", description = "스터디 활동 생성·조회·수정·삭제 API")
@RestController
@RequestMapping("/api/studies/{studyId}/activities")
@RequiredArgsConstructor
public class StudyActivityController {

    private final StudyActivityService studyActivityService;

    @Operation(
            summary = "스터디 활동 생성",
            description = "ACTIVE 스터디의 ACTIVE 멤버가 새로운 활동을 작성합니다."
    )
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

    @Operation(
            summary = "스터디 활동 목록 조회",
            description = "DRAFT가 아닌 스터디의 미삭제 활동을 최신순으로 조회합니다."
    )
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

    @Operation(
            summary = "스터디 활동 상세 조회",
            description = "DRAFT가 아닌 스터디의 미삭제 활동 상세 정보를 조회합니다."
    )
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

    @Operation(
            summary = "스터디 활동 수정",
            description = "ACTIVE 스터디에서 작성자 본인이 활동 내용을 수정합니다."
    )
    @PutMapping("/{activityId}")
    public StudyActivityResponse updateActivity(
            @PathVariable Long studyId,
            @PathVariable Long activityId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody StudyActivityUpdateRequest request
    ) {
        return studyActivityService.updateActivity(
                studyId,
                activityId,
                loginUserPrincipal.user().id(),
                request.content()
        );
    }

    @Operation(
            summary = "스터디 활동 삭제",
            description = "ACTIVE 스터디에서 작성자 본인이 활동을 소프트 삭제합니다."
    )
    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteActivity(
            @PathVariable Long studyId,
            @PathVariable Long activityId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal
    ) {
        studyActivityService.deleteActivity(
                studyId,
                activityId,
                loginUserPrincipal.user().id()
        );
    }
}
