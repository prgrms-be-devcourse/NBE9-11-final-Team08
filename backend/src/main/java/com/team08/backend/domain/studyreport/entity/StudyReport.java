package com.team08.backend.domain.studyreport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "study_reports")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyReport {
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
}
