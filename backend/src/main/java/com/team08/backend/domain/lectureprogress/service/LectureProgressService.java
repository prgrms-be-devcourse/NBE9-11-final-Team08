package com.team08.backend.domain.lectureprogress.service;

import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.dto.CourseLectureProgressResponse;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.event.LectureCompletedEvent;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureProgressService {

    private static final int MAX_WATCHED_DELTA_SECONDS = 600;   // 10분

    private static final int PLAYBACK_SPEED_TOLERANCE = 2;

    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentQueryService enrollmentQueryService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LectureProgress applyHeartbeat(Long userId, Long lectureId, int positionSeconds,
                                          int watchedDeltaSeconds, LocalDateTime eventTime) {
        // 실제 누적량은 applyProgress 에서 절대 상한 + 벽시계 경과 경계로 보정한다.
        return applyProgress(userId, lectureId, positionSeconds, watchedDeltaSeconds, eventTime);
    }

    // 강좌 커리큘럼 화면용 — 사용자의 강좌 내 강의별 진행도 목록(진행 이력이 있는 강의만 포함)
    @Transactional(readOnly = true)
    public List<CourseLectureProgressResponse> getCourseProgress(Long userId, Long courseId) {
        List<Long> lectureIds = lectureRepository.findIdsByCourseId(courseId);
        if (lectureIds.isEmpty()) {
            return List.of();
        }
        return lectureProgressRepository.findByUserIdAndLectureIdIn(userId, lectureIds).stream()
                .map(CourseLectureProgressResponse::from)
                .toList();
    }

    @Transactional
    public LectureProgress ensureStarted(Long userId, Lecture lecture, LocalDateTime now) {
        //활성 수강권이 있을 때만 진행 행을 만든다.
        LectureProgress existing = lectureProgressRepository
                .findByUserIdAndLectureId(userId, lecture.getId())
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        // null 을 반환한다(입장 자체는 막지 않음 — 메타데이터는 제공).
        if (!canStartProgress(userId, lecture)) {
            return null;
        }
        // 동시 입장(예: StrictMode 이중 마운트·더블클릭)으로 두 요청이 같은 행을 INSERT 하면
        // uk_lecture_progress_user_lecture 중복 키로 터졌다. 멱등 확보로 충돌을 흡수한다.
        return getOrCreateStarted(userId, lecture.getId(), 0, now);
    }

    @Transactional
    public void upsertLastPosition(Long userId, Long lectureId, int positionSeconds, LocalDateTime eventTime) {
        applyProgress(userId, lectureId, positionSeconds, 0, eventTime);
    }

    private LectureProgress applyProgress(Long userId, Long lectureId, int positionSeconds,
                                          int watchedDeltaSeconds, LocalDateTime eventTime) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        LectureProgress progress = lectureProgressRepository
                .findByUserIdAndLectureId(userId, lectureId)
                .orElse(null);

        LocalDateTime previousBeatAt = progress != null ? progress.getUpdatedAt() : null;
        boolean wasCompleted = progress != null && Boolean.TRUE.equals(progress.getCompleted());

        if (progress == null) {
            if (!canStartProgress(userId, lecture)) {
                throw new CustomException(ErrorCode.VIDEO_ACCESS_DENIED);
            }
            // 동시 생성(입장과 하트비트가 겹치는 경우 등)에 대비해 멱등으로 행을 확보한다.
            // previousBeatAt 은 null 로 유지해 최초 비트의 delta 산정 동작을 보존한다.
            progress = getOrCreateStarted(userId, lectureId, positionSeconds, eventTime);
        }

        int effectiveDelta = boundWatchedDelta(watchedDeltaSeconds, previousBeatAt, eventTime);
        progress.applyProgress(positionSeconds, effectiveDelta, lecture.getDurationSeconds(), eventTime);
        LectureProgress saved = lectureProgressRepository.save(progress);

        // completed 가 false→true 로 처음 전이된 경우에만 완료 이벤트를 발행한다.
        // (하트비트마다가 아니라, 진행률을 실제로 움직이는 완료 시점에만 리포트가 갱신됨)
        if (!wasCompleted && Boolean.TRUE.equals(saved.getCompleted())) {
            eventPublisher.publishEvent(new LectureCompletedEvent(
                    userId,
                    lecture.getChapter().getCourse().getId(),
                    lecture.getChapter().getId(),
                    lectureId,
                    eventTime
            ));
        }
        return saved;
    }

    /**
     * 진행 행을 동시성에 안전하게 확보한다. 없으면 멱등 INSERT 로 만들고, 동시 요청이 먼저
     * 만들었더라도 중복 키 예외 없이 그 행을 재조회해 영속(managed) 엔티티로 돌려준다.
     * INSERT 직후이므로 재조회는 항상 존재한다(없으면 내부 오류로 간주).
     */
    private LectureProgress getOrCreateStarted(Long userId, Long lectureId, int positionSeconds, LocalDateTime now) {
        lectureProgressRepository.insertIfAbsent(userId, lectureId, positionSeconds, now);
        return lectureProgressRepository
                .findByUserIdAndLectureId(userId, lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
    }

    private boolean canStartProgress(Long userId, Lecture lecture) {
        //맛보기 강의인지 검사
        if (lecture.isFreePreview()) {
            return true;
        }
        //수강권 확인
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
