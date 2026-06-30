package com.team08.backend.domain.course.controller;

import com.team08.backend.domain.chapter.dto.ChapterReorderRequest;
import com.team08.backend.domain.course.service.CurriculumService;
import com.team08.backend.domain.lecture.dto.LectureReorderRequest;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "강좌 커리큘럼 API", description = "강사용 강좌 챕터와 강의 순서 변경 API")
@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @Operation(
            summary = "강좌 챕터 순서 일괄 변경",
            description = "현재 로그인한 강사가 본인 강좌의 챕터 순서(orderNo)를 드래그 앤 드롭 결과대로 일괄 갱신합니다. 판매 중인 강좌는 커리큘럼 순서를 변경할 수 없습니다."
    )
    @PutMapping("/courses/{courseId}/chapters/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderChapters(
            @Parameter(description = "강좌 ID", example = "1") @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody ChapterReorderRequest request) {
        curriculumService.reorderChapters(courseId, loginUserPrincipal.user().id(), request);
    }

    @Operation(
            summary = "챕터 강의 순서 일괄 변경",
            description = "현재 로그인한 강사가 본인 챕터 안의 강의 순서(orderNo)를 드래그 앤 드롭 결과대로 일괄 갱신합니다. 판매 중인 강좌의 강의 순서는 변경할 수 없습니다."
    )
    @PutMapping("/chapters/{chapterId}/lectures/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderLectures(
            @Parameter(description = "챕터 ID", example = "1") @PathVariable("chapterId") Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody LectureReorderRequest request) {
        curriculumService.reorderLectures(chapterId, loginUserPrincipal.user().id(), request);
    }
}
