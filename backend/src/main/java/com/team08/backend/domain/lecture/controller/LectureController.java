package com.team08.backend.domain.lecture.controller;

import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.service.LectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters/{chapterId}/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long createLecture(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @Valid @RequestBody LectureCreateRequest request) {

        return lectureService.createLecture(courseId, chapterId, request);
    }
}
