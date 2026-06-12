package com.team08.backend.domain.aifeedback.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long studyId;

    @Column(nullable = false, unique = true)
    private Long studyActivityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiFeedbackStatus status;

    @Lob
    private String feedback;

    @Lob
    @Column(nullable = false)
    private String activityContentSnapshot;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String promptVersion;

    private AiFeedback(
            Long userId,
            Long studyId,
            Long studyActivityId,
            String activityContentSnapshot,
            String modelName,
            String promptVersion
    ) {
        this.userId = userId;
        this.studyId = studyId;
        this.studyActivityId = studyActivityId;
        this.activityContentSnapshot = activityContentSnapshot;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = AiFeedbackStatus.PROCESSING;
    }

    public static AiFeedback startProcessing(
            Long userId,
            Long studyId,
            Long studyActivityId,
            String activityContentSnapshot,
            String modelName,
            String promptVersion
    ) {
        return new AiFeedback(
                userId,
                studyId,
                studyActivityId,
                activityContentSnapshot,
                modelName,
                promptVersion
        );
    }

    public void complete(String feedback) {
        this.feedback = feedback;
        this.status = AiFeedbackStatus.COMPLETED;
    }

    public void fail() {
        this.status = AiFeedbackStatus.FAILED;
    }

    public void markStale() {
        if (status == AiFeedbackStatus.COMPLETED) {
            this.status = AiFeedbackStatus.STALE;
        }
    }

    public void restartProcessing(
            String activityContentSnapshot,
            String modelName,
            String promptVersion
    ) {
        this.activityContentSnapshot = activityContentSnapshot;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = AiFeedbackStatus.PROCESSING;
    }

    public boolean hasSameSnapshot(String content) {
        return activityContentSnapshot.equals(content);
    }
}
