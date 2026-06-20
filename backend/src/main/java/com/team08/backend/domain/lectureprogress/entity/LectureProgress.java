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

    /** progressRate(%) 가 이 값 이상이면 수강 완료로 본다. */
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

    /**
       - 진행 정보를 갱신한다. 하트비트(재생 중 주기 호출)와 강의 퇴장 둘 다 사용한다.
       - watchedSeconds: 실제 재생 경과초(delta)를 누적한다. 되감기·다시보기로 인한
           위치 중복 합산 문제가 없는 "진짜 시청 시간"이다. (퇴장 호출 시 delta=0)
       - progressRate: watchedSeconds/duration 기준 정수 퍼센트(0~100). 실제 재생 누적분으로
           측정하므로 끝으로 건너뛰기만 하는 조작에 강하다. watchedSeconds 가 단조 증가하므로 같이 단조 증가.
       - completed: progressRate 가 임계치 이상에 처음 도달한 시점에 1회 확정한다.
       - lastPositionSeconds: 이어보기용 마지막 위치.
     "최근 수강 강의" 정렬 기준인 updatedAt 도 함께 갱신한다.
     *
     * @param watchedDeltaSeconds 직전 호출 이후 실제로 재생된 초 (음수/null 은 0 으로 간주)
     * @param durationSeconds     강의 길이(초). 0 이하면 progressRate 산정을 건너뛴다.
     */
    public void applyProgress(int positionSeconds, int watchedDeltaSeconds, int durationSeconds, LocalDateTime now) {
        if (watchedDeltaSeconds > 0) {
            this.watchedSeconds += watchedDeltaSeconds;
        }
        this.lastPositionSeconds = positionSeconds;
        recomputeProgressRate(durationSeconds, now);
        this.updatedAt = now;
    }

    // 진행률은 실제 시청 누적초(watchedSeconds) 기준 정수 퍼센트로 산정한다(건너뛰기에 강함).
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
