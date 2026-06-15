package com.team08.backend.domain.lectureprogress.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_progresses", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lecture_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureProgress {
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
    @Column(precision = 5, scale = 2)
    private BigDecimal progressRate;
    @Column(nullable = false)
    private Boolean completed = false;
    private LocalDateTime completedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
