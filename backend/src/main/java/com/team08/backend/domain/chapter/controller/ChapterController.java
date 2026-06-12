package com.team08.backend.domain.chapter.controller;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long createChapter(
            @PathVariable Long courseId,
            @Valid @RequestBody ChapterCreateRequest request) {

        return chapterService.createChapter(courseId, request);
    }
}
