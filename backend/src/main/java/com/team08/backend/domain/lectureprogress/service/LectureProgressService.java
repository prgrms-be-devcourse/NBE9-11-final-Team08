package com.team08.backend.domain.lectureprogress.service;

import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LectureProgressService {

    /**
     * 하트비트 한 번에 누적할 수 있는 watchedSeconds 의 절대 상한(초).
     * 클라이언트 버그·조작으로 비정상적으로 큰 delta 가 들어와 집계가 오염되는 것을 막는다.
     */
    private static final int MAX_WATCHED_DELTA_SECONDS = 600;   // 10분

    /**
     * 벽시계 경과 대비 허용 배율. watchedSeconds 는 "실제 재생 경과초"이므로 직전 하트비트 이후
     * 실제 흐른 시간(서버 기준)보다 빠르게 쌓일 수 없다. 배속 재생·약간의 시계 오차를 감안해 2배까지 허용.
     * 이 경계 덕분에 연타·재전송으로 watchedSeconds 를 부풀리는 조작이 실제 시간 비용에 묶인다.
     */
    private static final int PLAYBACK_SPEED_TOLERANCE = 2;

    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentQueryService enrollmentQueryService;
    /**
     * 재생 중 하트비트로 진행 정보를 갱신한다. (재생 중 주기적으로 호출)
     * watchedSeconds 에 실제 재생 경과초(delta)를 누적하고, 진행률·완료 여부를 재계산한다.
     *
     * @param watchedDeltaSeconds 직전 하트비트 이후 실제로 재생된 초
     * @return 갱신된 진행 정보
     */
    @Transactional
    public LectureProgress applyHeartbeat(Long userId, Long lectureId, int positionSeconds,
                                          int watchedDeltaSeconds, LocalDateTime eventTime) {
        // 실제 누적량은 applyProgress 에서 절대 상한 + 벽시계 경과 경계로 보정한다.
        return applyProgress(userId, lectureId, positionSeconds, watchedDeltaSeconds, eventTime);
    }

    /**
     * 강의 입장 시 lectureProgress 행을 보장한다(없으면 서버 시각으로 생성). 첫 하트비트가 벽시계 경계를 적용할
     * 기준점(updatedAt)을 미리 서버가 찍어 두는 것이 목적이라, 시각은 클라이언트 값이 아닌 서버 시각을 쓴다.
     * 행이 이미 있으면 그대로 반환한다. 행을 새로 만들 때는 수강권을 검증하며,
     * 미등록·비(非)무료면 가짜 행을 만들지 않고 null 을 반환한다(입장 자체는 막지 않음 — 메타데이터는 제공).
     */
    @Transactional
    public LectureProgress ensureStarted(Long userId, Lecture lecture, LocalDateTime now) {
        LectureProgress existing = lectureProgressRepository
                .findByUserIdAndLectureId(userId, lecture.getId())
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        // 무료 맛보기이거나 활성 수강권이 있을 때만 진행 행을 만든다. 그 외에는 가짜 행을 만들지 않고
        // null 을 반환한다(입장 자체는 막지 않음 — 메타데이터는 제공).
        if (!canStartProgress(userId, lecture)) {
            return null;
        }
        return lectureProgressRepository.save(
                LectureProgress.start(userId, lecture.getId(), 0, now));
    }

    /**
     * 강의 퇴장 시 마지막 시청 위치를 저장한다. (LECTURE_EXIT 이벤트에서 호출)
     * 시청 시간 누적은 하트비트가 담당하므로 여기서는 delta 0 으로 위치·진행률만 마지막으로 반영한다.
     * (user_id, lecture_id) 진행 정보가 있으면 갱신하고, 없으면 새로 생성한다(upsert).
     * 엔티티에 unique(user_id, lecture_id) 제약이 있어 사용자·강의당 1행만 유지된다.
     */
    @Transactional
    public void upsertLastPosition(Long userId, Long lectureId, int positionSeconds, LocalDateTime eventTime) {
        applyProgress(userId, lectureId, positionSeconds, 0, eventTime);
    }

    private LectureProgress applyProgress(Long userId, Long lectureId, int positionSeconds,
                                          int watchedDeltaSeconds, LocalDateTime eventTime) {
        // 강의(영상 길이·강좌·무료여부)를 한 쿼리로 조회
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        // 이전 진행도를 가져옴
        LectureProgress progress = lectureProgressRepository
                .findByUserIdAndLectureId(userId, lectureId)
                .orElse(null);

        //마지막 heartbeat 시간
        LocalDateTime previousBeatAt = progress != null ? progress.getUpdatedAt() : null;

        // 이전 진행도가 없으면 권한있는 접근인지 확인  (= 강의 영상 첫 시청일 때)
        // 무료 맛보기이거나 활성 수강권이 있어야 새 행을 만들 수 있다.
        if (progress == null) {
            if (!canStartProgress(userId, lecture)) {
                throw new CustomException(ErrorCode.VIDEO_ACCESS_DENIED);
            }
            progress = LectureProgress.start(userId, lectureId, positionSeconds, eventTime);
        }

        // 요청값으로 들어온 증분시청시간에 대한 검증 후 apply
        int effectiveDelta = boundWatchedDelta(watchedDeltaSeconds, previousBeatAt, eventTime);
        progress.applyProgress(positionSeconds, effectiveDelta, lecture.getDurationSeconds(), eventTime);
        return lectureProgressRepository.save(progress);
    }

    /**
     * 누적할 watchedSeconds delta 를 보정한다.

     *  음수 제거 + 절대 상한(MAX_WATCHED_DELTA_SECONDS) 적용
     *  직전 하트비트 이후 실제로 흐른 시간(서버 기준) × 배속허용 이내로 제한 — 벽시계 경계.
     *  즉시 도착한 재전송·연타는 경과시간 ≈ 0 이라 기여 ≈ 0 이 되어 중복 누적·인플레이션을 막는다.

     * 첫 하트비트(신규 행)는 비교 기준점이 없어 절대 상한만 적용한다.
     */
    /**
     * 진행 행을 새로 만들 수 있는 접근인지 판단한다.
     * 무료 맛보기 강의는 수강권 없이도 허용하고, 그 외에는 해당 강좌에 활성 수강권이 있어야 한다.
     */
    // TODO: 무료 맛보기 강의 허용 여부 결정및 수정
    private boolean canStartProgress(Long userId, Lecture lecture) {
        if (lecture.isFreePreview()) {
            return true;
        }
        Long courseId = lecture.getChapter().getCourse().getId();
        return enrollmentQueryService.hasActiveEnrollment(userId, courseId);
    }

    private int boundWatchedDelta(int rawDelta, LocalDateTime previousBeatAt, LocalDateTime now) {
        int delta = Math.min(Math.max(rawDelta, 0), MAX_WATCHED_DELTA_SECONDS);
        if (previousBeatAt == null) {
            return delta;
        }
        long elapsedSeconds = Math.max(0, Duration.between(previousBeatAt, now).getSeconds());
        long allowed = elapsedSeconds * PLAYBACK_SPEED_TOLERANCE;
        return (int) Math.min((long) delta, allowed);
    }
}
