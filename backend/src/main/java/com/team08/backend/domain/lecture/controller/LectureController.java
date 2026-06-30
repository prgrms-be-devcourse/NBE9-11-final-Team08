package com.team08.backend.domain.lecture.controller;

import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.dto.LectureDetailResponse;
import com.team08.backend.domain.media.dto.VideoStreamResponse;
import com.team08.backend.domain.lecture.service.LectureService;
import com.team08.backend.domain.media.service.VideoAccessService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters/{chapterId}/lectures")
@RequiredArgsConstructor
@Tag(name = "강의 API", description = "강의 생성, 강의 상세 조회, 영상 스트리밍 URL 발급 API")
public class LectureController {

    private final LectureService lectureService;
    private final VideoAccessService videoAccessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "강의 생성", description = "현재 로그인한 강사가 본인 강좌의 특정 챕터에 새 강의를 생성합니다.")
    public Long createLecture(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable Long courseId,
            @Parameter(description = "챕터 ID", example = "1") @PathVariable Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody LectureCreateRequest request) {
        return lectureService.createLecture(courseId, chapterId, principal.user().id(), request);
    }

    @GetMapping("/{lectureId}/stream")
    @Operation(summary = "강의 영상 스트리밍 URL 조회", description = "강의 시청 권한을 검증하고 HLS 스트리밍 경로와 필요한 쿠키 헤더를 발급합니다. 관리자 미리보기에서는 관리자 권한으로 영상 접근을 허용합니다.")
    public ResponseEntity<String> getVideoStreamUrl(
            @Parameter(description = "강의 ID", example = "1") @PathVariable Long lectureId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        Long userId = loginUserPrincipal != null ? loginUserPrincipal.user().id() : null;
        VideoStreamResponse streamResponse = videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId);
        return ResponseEntity.ok()
                .headers(streamResponse.asHttpHeaders())
                .body(streamResponse.path());
    }

    @GetMapping("/{lectureId}")
    @Operation(summary = "강의 상세 조회", description = "강좌와 챕터에 속한 특정 강의의 상세 정보를 조회합니다. 무료 미리보기 여부와 수강 권한에 따라 영상 정보 노출이 달라질 수 있습니다.")
    public LectureDetailResponse getLectureDetail(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable Long courseId,
            @Parameter(description = "챕터 ID", example = "1") @PathVariable Long chapterId,
            @Parameter(description = "강의 ID", example = "1") @PathVariable Long lectureId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        Long userId = principal != null ? principal.user().id() : null;
        return lectureService.getLectureDetail(courseId, chapterId, lectureId, userId);
    }
}
