package com.team08.backend.domain.chapter.controller;

import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.service.ChapterService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "챕터", description = "챕터 조회 및 강의 입장 API")
@RestController
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @Operation(
            summary = "챕터 리스트 조회",
            description = "코스에 속한 챕터 목록을 강의 목록과 함께 순서대로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/courses/{courseId}/chapters")
    public List<ChapterWithLecturesResponse> getChapters(
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return chapterService.getChaptersWithLectures(courseId);
    }

    @Operation(
            summary = "챕터 첫 강의 입장",
            description = "선택한 챕터의 첫 번째 강의 러닝 스페이스로 입장합니다. 학습 진행 정보(progress)가 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입장 성공"),
            @ApiResponse(responseCode = "404", description = "챕터 또는 강의를 찾을 수 없음")
    })
    @GetMapping("/api/chapters/{chapterId}/lectures/first")
    public LectureEnterResponse enterFirstLecture(
            @Parameter(description = "챕터 ID") @PathVariable Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return chapterService.enterFirstLecture(chapterId, principal.user().id());
    }

    @Operation(
            summary = "최근 수강 강의 입장",
            description = "챕터 내에서 사용자가 가장 최근 학습한 강의 러닝 스페이스로 입장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입장 성공"),
            @ApiResponse(responseCode = "404", description = "챕터 / 강의 / 최근 학습 이력을 찾을 수 없음")
    })
    @GetMapping("/api/chapters/{chapterId}/lectures/recent")
    public LectureEnterResponse enterRecentLecture(
            @Parameter(description = "챕터 ID") @PathVariable Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return chapterService.enterRecentLecture(chapterId, principal.user().id());
    }
}
