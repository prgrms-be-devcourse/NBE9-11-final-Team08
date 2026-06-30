package com.team08.backend.domain.lectureprogress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_progresses", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lecture_id"}))
@Getter
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

    // 하트비트 누적(watchedSeconds += delta)의 lost update 방어용 낙관적 락 버전.
    // 동시 하트비트가 같은 행을 read-modify-write 하면 UPDATE 의 version 검증에서 한쪽이 실패하고,
    // 서비스가 재조회→재가산으로 재시도한다. 신규 엔티티는 null→영속 시 0 으로 채워지고,
    // 네이티브 insertIfAbsent 로 만든 행은 version = 0 을 명시 삽입한다.
    @Version
    private Long version;

    // 생성자는 기존 10개 인자 시그니처를 그대로 유지한다(version 은 영속 계층이 관리하므로 제외).
    // start() 팩토리·DataSeeder·테스트가 이 시그니처로 행을 만든다.
    public LectureProgress(Long id, Long lectureId, Long userId,
                           Integer lastPositionSeconds, Integer watchedSeconds, Integer progressRate,
                           Boolean completed, LocalDateTime completedAt,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.lectureId = lectureId;
        this.userId = userId;
        this.lastPositionSeconds = lastPositionSeconds;
        this.watchedSeconds = watchedSeconds;
        this.progressRate = progressRate;
        this.completed = completed;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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
