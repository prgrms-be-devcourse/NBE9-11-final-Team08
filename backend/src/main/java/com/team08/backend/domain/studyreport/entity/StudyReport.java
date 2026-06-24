package com.team08.backend.domain.studyreport.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyReport extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long studyId;
    private Integer totalWatchTime;
    private Integer totalQnaCount;
    // progressRate 는 completedLectures/totalLectures 의 파생값이라 저장하지 않는다(필요 시 계산).
    private Integer completedLectures;
    private Integer totalLectures;
    private Integer studyDays;
    @Column(columnDefinition = "TEXT")
    private String topLectures;      // JSON: [{lectureId, title, watchTimeSeconds}]
    @Column(columnDefinition = "TEXT")
    private String dailyProgress;    // JSON: [{date, progressRate}]
    @Column(columnDefinition = "TEXT")
    private String dailyActivityMap; // JSON: {"2026-06-07": 3, ...}

    public static StudyReport create(Long userId, Long studyId) {
        StudyReport r = new StudyReport();
        r.userId = userId;
        r.studyId = studyId;
        return r;
    }

    public void update(
            Integer totalWatchTime,
            Integer totalQnaCount,
            Integer completedLectures,
            Integer totalLectures,
            Integer studyDays,
            String topLectures,
            String dailyProgress,
            String dailyActivityMap
    ) {
        this.totalWatchTime = totalWatchTime;
        this.totalQnaCount = totalQnaCount;
        this.completedLectures = completedLectures;
        this.totalLectures = totalLectures;
        this.studyDays = studyDays;
        this.topLectures = topLectures;
        this.dailyProgress = dailyProgress;
        this.dailyActivityMap = dailyActivityMap;
    }
}
