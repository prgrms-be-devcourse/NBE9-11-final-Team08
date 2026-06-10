package com.team08.backend.domain.aifeedback.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_feedbacks")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long studyId;
    @Column(nullable = false, unique = true)
    private Long studyActivityId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AiFeedbackStatus status;
    @Lob
    private String feedback;
    @Column(nullable = false)
    private String modelName;
    @Column(nullable = false)
    private String promptVersion;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
