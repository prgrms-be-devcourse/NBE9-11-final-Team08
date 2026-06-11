package com.team08.backend.domain.lectureqna.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long lectureId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, length = 255)
    private String title;
    @Lob @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private QnaQuestion(
            Long userId,
            Long lectureId,
            String title,
            String content
    ) {
        this.lectureId = lectureId;
        this.userId = userId;
        this.title = title;
        this.content = content;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static QnaQuestion create(
            Long userId,
            Long lectureId,
            String title,
            String content
    ) {
        return new QnaQuestion(
                userId,
                lectureId,
                title,
                content
        );
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}