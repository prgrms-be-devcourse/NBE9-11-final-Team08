package com.team08.backend.domain.lecturereflection.controller;

import com.team08.backend.domain.lecturereflection.dto.LectureReflectionRequest;
import com.team08.backend.domain.lecturereflection.dto.LectureReflectionResponse;
import com.team08.backend.domain.lecturereflection.service.LectureReflectionService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회고", description = "강의별 회고 작성·수정·조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lectures/{lectureId}/reflections")
public class LectureReflectionController {

    private final LectureReflectionService reflectionService;

    @Operation(
            summary = "회고 작성",
            description = "강의별 회고를 작성합니다. (사용자, 강의) 기준으로 1개만 작성 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 작성된 회고 존재")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public LectureReflectionResponse createReflection(
            @Parameter(description = "강의 ID") @PathVariable Long lectureId,
            @Valid @RequestBody LectureReflectionRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return reflectionService.createReflection(principal.user().id(), lectureId, request.content());
    }

    @Operation(
            summary = "회고 수정",
            description = "작성자 본인만 회고를 수정할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회고를 찾을 수 없음")
    })
    @PutMapping("/{reflectionId}")
    public LectureReflectionResponse updateReflection(
            @Parameter(description = "강의 ID") @PathVariable Long lectureId,
            @Parameter(description = "회고 ID") @PathVariable Long reflectionId,
            @Valid @RequestBody LectureReflectionRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return reflectionService.updateReflection(reflectionId, principal.user().id(), request.content());
    }

    @Operation(
            summary = "회고 조회",
            description = "사용자와 강의 기준으로 본인의 회고를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회고를 찾을 수 없음")
    })
    @GetMapping
    public LectureReflectionResponse getReflection(
            @Parameter(description = "강의 ID") @PathVariable Long lectureId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return reflectionService.getReflection(principal.user().id(), lectureId);
    }
}
