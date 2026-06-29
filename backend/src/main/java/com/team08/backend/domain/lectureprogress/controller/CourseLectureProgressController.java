package com.team08.backend.domain.lectureprogress.controller;

import com.team08.backend.domain.lectureprogress.dto.CourseLectureProgressResponse;
import com.team08.backend.domain.lectureprogress.service.LectureProgressService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "강의 진행", description = "강의별 시청 진행 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/lectures/progress")
public class CourseLectureProgressController {

    private final LectureProgressService lectureProgressService;

    @Operation(summary = "강좌 내 강의별 진행도 목록 조회",
               description = "강좌 커리큘럼 화면에서 사용자가 학습한 강의들의 진행률/완료 여부를 한 번에 조회한다. "
                       + "진행 이력이 없는 강의는 응답에 포함되지 않는다(프론트에서 0%로 처리).")
    @GetMapping
    public List<CourseLectureProgressResponse> getCourseProgress(
            @Parameter(description = "강좌 ID") @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return lectureProgressService.getCourseProgress(principal.user().id(), courseId);
    }
}
