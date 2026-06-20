package com.team08.backend.domain.lectureprogress.controller;

import com.team08.backend.domain.lectureprogress.dto.LectureProgressResponse;
import com.team08.backend.domain.lectureprogress.dto.LectureProgressUpdateRequest;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.service.LectureProgressService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "강의 진행", description = "강의별 시청 진행 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lectures/{lectureId}/progress")
public class lectureProgressController {

    private final LectureProgressService lectureProgressService;

    @Operation(summary = "강의 진행 정보 갱신(하트비트)",
               description = "재생 중 주기적으로, 그리고 강의를 떠날 때 호출하여 시청 시간·진행률을 누적한다.")
    @PatchMapping
    public LectureProgressResponse updateProgress(
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureProgressUpdateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        LectureProgress progress = lectureProgressService.applyHeartbeat(
                principal.user().id(),
                lectureId,
                request.positionSeconds(),
                request.watchedDeltaSeconds(),
                LocalDateTime.now()
        );
        return LectureProgressResponse.from(progress);
    }
}
