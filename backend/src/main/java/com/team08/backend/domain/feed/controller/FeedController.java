package com.team08.backend.domain.feed.controller;

import com.team08.backend.domain.feed.dto.response.FeedCursorResponse;
import com.team08.backend.domain.feed.service.FeedService;
import com.team08.backend.domain.feed.sse.FeedSseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/feed")
public class FeedController {

    private final FeedService feedService;
    private final FeedSseService feedSseService;

    @GetMapping
    public FeedCursorResponse getFeed(
            @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorOccurredAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        return feedService.getFeedItems(
                studyId,
                loginUserPrincipal.user().id(),
                cursorOccurredAt,
                cursorId,
                size
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeed(
            @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId,
            HttpServletResponse response
    ) {
        // 프록시(nginx 등)가 SSE 응답을 버퍼링하면 이벤트가 실시간으로 흐르지 않고
        // 버퍼가 찰 때까지 지연·뭉텅이로 전달된다(드레인/실시간 푸시가 깨짐).
        // X-Accel-Buffering: no 는 nginx 가 이 응답만 버퍼링/캐시하지 않도록 지시한다.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        return feedSseService.subscribe(
                studyId,
                loginUserPrincipal.user().id(),
                lastEventId
        );
    }
}
