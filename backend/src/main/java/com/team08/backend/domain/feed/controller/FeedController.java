package com.team08.backend.domain.feed.controller;

import com.team08.backend.domain.feed.dto.response.FeedCursorResponse;
import com.team08.backend.domain.feed.service.FeedService;
import com.team08.backend.domain.feed.sse.FeedSseService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "스터디 피드 API", description = "스터디 활동 피드 조회 및 실시간 스트리밍 관련 API")
public class FeedController {

    private final FeedService feedService;
    private final FeedSseService feedSseService;

    @Operation(
            summary = "스터디 피드 목록 조회",
            description = """
                    스터디의 활동 피드를 커서 기반 페이지네이션으로 조회한다.
                    최초 조회 시 커서 파라미터를 비우면 최신 항목부터 반환하며,
                    응답의 nextCursor(occurredAt, id)를 다음 요청의 커서로 전달해 이어서 조회한다.
                    hasNext 가 false 면 더 이상 조회할 항목이 없다.
                    ACTIVE 스터디 멤버만 조회할 수 있다.
                    """)
    @GetMapping
    public FeedCursorResponse getFeed(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Parameter(description = "커서: 마지막으로 받은 항목의 발생 시각(ISO-8601). 최초 조회 시 생략한다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorOccurredAt,
            @Parameter(description = "커서: 마지막으로 받은 항목의 ID. 발생 시각이 동일한 항목 간 순서를 보정한다. 최초 조회 시 생략한다.")
            @RequestParam(required = false) Long cursorId,
            @Parameter(description = "한 번에 조회할 항목 수(기본값 10)")
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

    @Operation(
            summary = "스터디 피드 실시간 스트리밍 (SSE)",
            description = """
                    스터디 피드 이벤트를 Server-Sent Events 로 실시간 구독한다.
                    새 활동이 발생하면 연결된 클라이언트로 즉시 푸시된다.
                    재연결 시 Last-Event-ID 헤더로 마지막 수신 이벤트 ID 를 전달하면 그 이후 누락된 이벤트를 드레인해 전달한다.
                    응답은 text/event-stream 이며, 프록시 버퍼링 방지를 위해 X-Accel-Buffering: no, Cache-Control: no-cache 헤더가 설정된다.
                    ACTIVE 스터디 멤버만 구독할 수 있다.
                    """)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeed(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Parameter(description = "재연결 시 마지막으로 수신한 이벤트 ID. 이 값 이후의 누락 이벤트를 드레인한다.")
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
