package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.chapter.dto.ChapterReorderRequest;
import com.team08.backend.domain.lecture.dto.LectureReorderRequest;
import com.team08.backend.domain.course.service.CurriculumService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Curriculum", description = "판매자용 강좌 커리큘럼 순서 제어 API")
@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @Operation(summary = "강좌 내 챕터 순서 일괄 변경", description = "드래그 앤 드롭으로 재정렬된 강좌 내 챕터들의 순서(orderNo)를 일괄 갱신합니다.")
    @PutMapping("/courses/{courseId}/chapters/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderChapters(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody ChapterReorderRequest request) {
        curriculumService.reorderChapters(courseId, loginUserPrincipal.user().id(), request);
    }

    @Operation(summary = "챕터 내 강의 순서 일괄 변경", description = "드래그 앤 드롭으로 재정렬된 챕터 내 강의들의 순서(orderNo)를 일괄 갱신합니다.")
    @PutMapping("/chapters/{chapterId}/lectures/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderLectures(
            @PathVariable("chapterId") Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody LectureReorderRequest request) {
        curriculumService.reorderLectures(chapterId, loginUserPrincipal.user().id(), request);
    }
}