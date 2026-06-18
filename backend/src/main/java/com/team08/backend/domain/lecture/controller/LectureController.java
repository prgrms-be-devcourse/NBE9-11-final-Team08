package com.team08.backend.domain.lecture.controller;

import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.service.LectureService;
import com.team08.backend.domain.lecture.service.VideoAccessService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters/{chapterId}/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;
    private final VideoAccessService videoAccessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long createLecture(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @Valid @RequestBody LectureCreateRequest request) {

        return lectureService.createLecture(courseId, chapterId, request);
    }

    @GetMapping("/{lectureId}/stream")
    public ResponseEntity<String> getVideoStreamUrl(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {

        VideoAccessService.VideoStreamResponse streamResponse = videoAccessService.verifyAndGenerateStreamCookies(lectureId, loginUserPrincipal.user().id());

        return ResponseEntity.ok()
                .headers(headers -> streamResponse.cookies()
                        .forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie.toString())))
                .body(streamResponse.path());
    }
}