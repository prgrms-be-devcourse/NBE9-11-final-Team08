package com.team08.backend.domain.studyreport.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * (사용자, 강좌, 날짜)별 학습 활동 일별 롤업. 학습 이벤트 적재마다 UPSERT 로 증분된다.
 * <p>
 * 리포트의 학습일수·일별진도·일별활동맵을 learning_events GROUP BY 스캔 없이 이 테이블에서 읽는다.
 * 진행률 시계열은 {@code completedCount} 를 날짜순 누적해 계산한다.
 * 갱신은 네이티브 UPSERT({@code StudyDailyStatRepository.upsertIncrement})로만 수행한다.
 */
@Entity
@Table(
        name = "learning_daily_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_user_course_date",
                columnNames = {"user_id", "course_id", "activity_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "event_count", nullable = false)
    private int eventCount;

    @Column(name = "completed_count", nullable = false)
    private int completedCount;
}
