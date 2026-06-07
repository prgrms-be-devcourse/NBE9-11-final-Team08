package com.team08.backend.domain.chapter.controller;

import com.team08.backend.domain.lecture.dto.LearningSpaceResponse;
import com.team08.backend.domain.lecture.service.LearningSpaceService;
import com.team08.backend.global.error.ErrorResponse;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/chapters")
@Tag(name = "Chapter Learning", description = "챕터 기반 러닝스페이스 진입 API")
public class ChapterLearningController {

    private final LearningSpaceService learningSpaceService;

    @Operation(
            summary = "챕터 입장",
            description = "챕터에서 기존 학습 기록이 있으면 마지막 학습 강의로, 처음 학습하는 챕터이면 첫 강의 러닝스페이스로 진입합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "러닝스페이스 진입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 챕터 ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "챕터 또는 강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{chapterId}/enter")
    public LearningSpaceResponse enterChapter(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "챕터 ID", example = "1")
            @Positive
            @PathVariable Long chapterId
    ) {
        return learningSpaceService.enterChapter(principal.user().id(), chapterId);
    }
}
