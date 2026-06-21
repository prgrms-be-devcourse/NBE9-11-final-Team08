package com.team08.backend.domain.studyreport.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

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
    @Column(precision = 5, scale = 2)
    private BigDecimal progressRate;
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
            BigDecimal progressRate,
            Integer studyDays,
            String topLectures,
            String dailyProgress,
            String dailyActivityMap
    ) {
        this.totalWatchTime = totalWatchTime;
        this.totalQnaCount = totalQnaCount;
        this.progressRate = progressRate;
        this.studyDays = studyDays;
        this.topLectures = topLectures;
        this.dailyProgress = dailyProgress;
        this.dailyActivityMap = dailyActivityMap;
    }
}
