package com.team08.backend.domain.studyactivity.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyActivity extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studyId;

    @Column(nullable = false)
    private Long authorId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime deletedAt;

    private StudyActivity(Long studyId, Long authorId, String content) {
        this.studyId = studyId;
        this.authorId = authorId;
        this.content = content;
    }

    public static StudyActivity create(Long studyId, Long authorId, String content) {
        return new StudyActivity(studyId, authorId, content);
    }

    public void update(String content) {
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
