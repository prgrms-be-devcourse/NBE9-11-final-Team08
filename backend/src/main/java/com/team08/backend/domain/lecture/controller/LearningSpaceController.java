package com.team08.backend.domain.lecture.controller;

import com.team08.backend.domain.lecture.dto.CommentCreateRequest;
import com.team08.backend.domain.lecture.dto.CommentResponse;
import com.team08.backend.domain.lecture.dto.LearningSpaceResponse;
import com.team08.backend.domain.lecture.dto.ProgressResponse;
import com.team08.backend.domain.lecture.dto.ProgressUpdateRequest;
import com.team08.backend.domain.lecture.dto.ReflectionResponse;
import com.team08.backend.domain.lecture.dto.ReflectionUpsertRequest;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/lectures")
@Tag(name = "Learning Space", description = "강의 러닝스페이스, 진행률, 회고, 댓글 API")
public class LearningSpaceController {

    private final LearningSpaceService learningSpaceService;

    @Operation(summary = "최근 수강한 강의 러닝스페이스 입장")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "러닝스페이스 진입 성공"),
            @ApiResponse(responseCode = "404", description = "최근 수강한 강의가 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/recent/learning-space")
    public LearningSpaceResponse enterRecentLecture(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningSpaceService.enterRecentLecture(principal.user().id());
    }

    @Operation(summary = "강의 러닝스페이스 조회", description = "강의 영상 정보, 현재 사용자의 진행 상태, 회고를 함께 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "러닝스페이스 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 강의 ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{lectureId}/learning-space")
    public LearningSpaceResponse getLearningSpace(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "강의 ID", example = "1")
            @Positive
            @PathVariable Long lectureId
    ) {
        return learningSpaceService.getLearningSpace(principal.user().id(), lectureId);
    }

    @Operation(summary = "강의 재생 위치 갱신", description = "프론트에서 5분마다 호출해 마지막 재생 위치를 저장합니다. 95% 이상 시청하거나 completed=true면 수강 완료 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진행 상태 갱신 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{lectureId}/progress")
    public ProgressResponse updateProgress(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "강의 ID", example = "1")
            @Positive
            @PathVariable Long lectureId,
            @Valid @RequestBody ProgressUpdateRequest request
    ) {
        return learningSpaceService.updateProgress(principal.user().id(), lectureId, request);
    }

    @Operation(summary = "강의 회고 조회", description = "현재 사용자가 해당 강의에 작성한 회고를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회고 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 강의 ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회고 또는 강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{lectureId}/reflection")
    public ReflectionResponse getReflection(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "강의 ID", example = "1")
            @Positive
            @PathVariable Long lectureId
    ) {
        return learningSpaceService.getReflection(principal.user().id(), lectureId);
    }

    @Operation(summary = "강의 회고 작성/수정", description = "회고는 사용자와 강의 조합당 하나만 유지되며, 이미 있으면 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회고 작성 또는 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{lectureId}/reflection")
    public ReflectionResponse upsertReflection(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "강의 ID", example = "1")
            @Positive
            @PathVariable Long lectureId,
            @Valid @RequestBody ReflectionUpsertRequest request
    ) {
        return learningSpaceService.upsertReflection(principal.user().id(), lectureId, request);
    }

    @Operation(summary = "강의 댓글 조회", description = "afterId를 전달하면 해당 댓글 ID 이후에 작성된 댓글만 조회해 댓글 새로고침에 사용할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{lectureId}/comments")
    public List<CommentResponse> getComments(
            @Parameter(description = "강의 ID", example = "1")
            @Positive
            @PathVariable Long lectureId,
            @Parameter(description = "이 댓글 ID 이후의 댓글만 조회", example = "15")
            @Positive
            @RequestParam(required = false) Long afterId
    ) {
        return learningSpaceService.getComments(lectureId, afterId);
    }

    @Operation(summary = "강의 댓글 작성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패 또는 부모 댓글 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{lectureId}/comments")
    public CommentResponse createComment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "강의 ID", example = "1")
            @Positive
            @PathVariable Long lectureId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return learningSpaceService.createComment(principal.user().id(), lectureId, request);
    }
}
