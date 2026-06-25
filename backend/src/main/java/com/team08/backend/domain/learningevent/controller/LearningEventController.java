package com.team08.backend.domain.learningevent.controller;

import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.service.LearningEventService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "학습 이벤트", description = "학습 활동 기록 및 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning-events")
public class LearningEventController {

    private final LearningEventService learningEventService;

    // ── 이벤트 기록 ─────────────────────────────────────────────────────
    @Operation(summary = "학습 이벤트 기록",
               description = "강의 입장, 영상 시청, 재생 위치, 수강 완료이벤트를 기록합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public LearningEventResponse recordEvent(
            @Valid @RequestBody RecordLearningEventRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
            // 적재 후 LearningEventRecorded 가 발행되고, 각 반응 리스너가 자기 타입만 처리한다.
            // 예: LECTURE_EXIT 시 ExitProgressFlushListener 가 커밋 후 best-effort 로 위치를 flush.
    ) {
        return learningEventService.recordEvent(principal.user().id(), request);
    }

    // ── 사용자별 활동 조회 ─────────────────────────────────────────────────
    @Operation(summary = "사용자별 학습 활동 조회",
               description = "본인 또는 관리자가 특정 사용자의 학습 이벤트 이력을 조회합니다.")
    @GetMapping("/users/{userId}/activities")
    public Page<LearningEventResponse> getUserActivities(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getUserActivities(
                principal.user().id(), userId, principal.user().role(), pageable
        );
    }

    // TODO: (멘토님:강은혜)데이터가 많은 것을 한번에 조회해야할 상황이 많음
    // 이서버 구조로 가면, 서비스& 어드민 정보 조회 양이 비슷함
        // ex) 어드민의 조회가 서비스 조회에 영향을 줄 수 있음
        // 어드민 API 와 서비스 API 를 구분해보면 좋음 -> 애초에 서버가 따로 뜨게 될 것임-> 여기서 설계를 어떻게 바꿔 볼 수 있을까?를 고민해보기
            //엔트포인트 나누는 전략을 단계적으로 세워보기(같은 엔드포인트를 여럿이 쓴다면? 엔드포인트 이름도 정의 하면 좋을 것 같음)
            //그럼에도 admin에서 속도가 빨라야함
            //mockdata를 1000개씩 돌려보면서 쿼리가 얼마나 느린지를 체감해보기
            //다같이 테스트 하는 부분을 넣어보면 좋을 것같음
    // 다같이 백엔드 개발을 하는 입장으로써 특정 CI 시 테스트 || 테스트없이 slow query를 걸러내는 방법을 고민 -> 백엔드 협업으로써 톤앤매너 획득
        // 한발 더 나아가 팀 관점으로 개선하는 것이 중요 -> 시스템화 (나 할 거 하면서 팀에 기여하는 것이... 목적&기능 백엔드 개발자)
        // 디테일을 좀더 챙겨 볼 것


    // ── 강의별 학습 통계 조회 ────────────────────────────────────────────────────────
    @Operation(summary = "강의별 학습 통계",
               description = "강좌 단위의 입장 수, 시청 시간, 수강 완료 수 등을 조회합니다. 관리자 또는 강좌 소유 판매자만 접근 가능합니다.")
    @GetMapping("/courses/{courseId}/stats")
    public CourseStatsResponse getCourseStats(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getCourseStats(
                principal.user().id(), courseId, principal.user().role()
        );
    }

    // ── 챕터별 학습 통계 조회 ───────────────────────────────────────────────────────
    @Operation(summary = "챕터별 학습 통계",
               description = "챕터 단위의 입장 수, 완료 수, 평균 시청 시간을 조회합니다. 관리자 또는 판매자만 접근 가능합니다.")
    @GetMapping("/chapters/{chapterId}/stats")
    public ChapterStatsResponse getChapterStats(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getChapterStats(
                principal.user().id(), chapterId, principal.user().role()
        );
    }

    // ── 관리자 전체 조회 ──────────────────────────────────────────────────
    @Operation(summary = "[관리자] 전체 학습 이벤트 조회",
               description = "모든 강좌의 학습 이벤트를 페이지네이션으로 조회합니다. 관리자 전용입니다.")
    @GetMapping("/admin")
    public Page<LearningEventResponse> getAllEvents(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getAllEvents(principal.user().role(), pageable);
    }

    // ── 판매자 강좌 필터링 조회 ────────────────────────────────────────────────
    @Operation(summary = "[판매자] 내 강좌 학습 이벤트 조회",
               description = "판매자 본인이 개설한 강좌의 학습 이벤트만 조회합니다.")
    @GetMapping("/seller")
    public Page<LearningEventResponse> getSellerEvents(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return learningEventService.getSellerEvents(
                principal.user().id(), principal.user().role(), pageable
        );
    }
}
