package com.team08.backend.domain.lecture.controller;

import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.lecture.service.LectureService;
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

@Tag(name = "강의 입장", description = "강의 러닝 스페이스 입장 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lectures")
public class LectureEnterController {

    private final LectureService lectureService;

    @Operation(summary = "특정 강의 입장",
               description = "특정 강의 러닝 스페이스로 입장한다. 강의 메타데이터와 학습 진행 정보(progress)를 반환하며, "
                       + "수강권이 있고 진행 이력이 없으면 진행 행을 생성한다.")
    @GetMapping("/{courseId}/{chapterId}/{lectureId}/enter")
    public LectureEnterResponse enterLecture(
            @Parameter(description = "강의 ID")
            @PathVariable Long courseId, @PathVariable Long chapterId,@PathVariable Long lectureId,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return lectureService.enterLecture(courseId,chapterId,lectureId, principal.user().id());
    }
}
