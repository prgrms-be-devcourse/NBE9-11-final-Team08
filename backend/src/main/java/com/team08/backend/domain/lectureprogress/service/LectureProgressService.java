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

    private static final int MAX_WATCHED_DELTA_SECONDS = 600;   // 10분

    private static final int PLAYBACK_SPEED_TOLERANCE = 2;

    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentQueryService enrollmentQueryService;

    @Transactional
    public LectureProgress applyHeartbeat(Long userId, Long lectureId, int positionSeconds,
                                          int watchedDeltaSeconds, LocalDateTime eventTime) {
        // 실제 누적량은 applyProgress 에서 절대 상한 + 벽시계 경과 경계로 보정한다.
        return applyProgress(userId, lectureId, positionSeconds, watchedDeltaSeconds, eventTime);
    }

    @Transactional
    public LectureProgress ensureStarted(Long userId, Lecture lecture, LocalDateTime now) {
        // 무료 맛보기이거나 활성 수강권이 있을 때만 진행 행을 만든다.
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
        return lectureProgressRepository.save(
                LectureProgress.start(userId, lecture.getId(), 0, now));
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

        if (progress == null) {
            if (!canStartProgress(userId, lecture)) {
                throw new CustomException(ErrorCode.VIDEO_ACCESS_DENIED);
            }
            progress = LectureProgress.start(userId, lectureId, positionSeconds, eventTime);
        }

        int effectiveDelta = boundWatchedDelta(watchedDeltaSeconds, previousBeatAt, eventTime);
        progress.applyProgress(positionSeconds, effectiveDelta, lecture.getDurationSeconds(), eventTime);
        return lectureProgressRepository.save(progress);
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
