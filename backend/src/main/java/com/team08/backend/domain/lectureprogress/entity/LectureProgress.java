package com.team08.backend.domain.lectureprogress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_progresses", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lecture_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureProgress {

    // progressRate 가 이 값 이상이면 수강 완료로 본다(%)
    private static final int COMPLETE_THRESHOLD_PERCENT = 90;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long lectureId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Integer lastPositionSeconds = 0;
    @Column(nullable = false)
    private Integer watchedSeconds = 0;
    @Column(nullable = false)
    private Integer progressRate = 0;   // 0~100 정수 퍼센트 (진행도 표시용이라 소수점 불필요)
    @Column(nullable = false)
    private Boolean completed = false;
    private LocalDateTime completedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 해당 강의의 첫 진행 정보 기록
    // watchedSeconds/progressRate 등 누적 지표는 아직 0 으로 두고, 마지막 위치만 기록한다.
    public static LectureProgress start(Long userId, Long lectureId, int positionSeconds, LocalDateTime now) {
        return new LectureProgress(
                null, lectureId, userId,
                positionSeconds, 0, 0,
                false, null, now, now);
    }

    public void applyProgress(int positionSeconds, int watchedDeltaSeconds, int durationSeconds, LocalDateTime now) {
        if (watchedDeltaSeconds > 0) {
            this.watchedSeconds += watchedDeltaSeconds;
        }
        this.lastPositionSeconds = positionSeconds;
        recomputeProgressRate(durationSeconds, now);
        this.updatedAt = now;
    }

    private void recomputeProgressRate(int durationSeconds, LocalDateTime now) {
        if (durationSeconds <= 0) {
            return;
        }
        int rate = (int) Math.min(100L, (long) this.watchedSeconds * 100 / durationSeconds);
        this.progressRate = rate;

        if (!this.completed && rate >= COMPLETE_THRESHOLD_PERCENT) {
            this.completed = true;
            this.completedAt = now;
        }
    }
}
