package com.team08.backend.domain.learningevent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_events")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Long lectureId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private LearningEventType eventType;
    private Integer positionSeconds;
    @Column(nullable = false)
    private LocalDateTime eventTime;
    @Column(unique = true)
    private String uniqueEventKey;
}
