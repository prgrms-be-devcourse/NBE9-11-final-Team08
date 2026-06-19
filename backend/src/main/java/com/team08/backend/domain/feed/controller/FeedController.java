package com.team08.backend.domain.feed.controller;

import com.team08.backend.domain.feed.dto.response.FeedCursorResponse;
import com.team08.backend.domain.feed.service.FeedService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/feed")
public class FeedController {

    private final FeedService feedService;

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
}
