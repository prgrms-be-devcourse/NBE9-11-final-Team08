package com.team08.backend.domain.lectureqna.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna_answers")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long questionId;
    @Column(nullable = false)
    private Long instructorId;
    @Lob @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private QnaAnswer(
            Long questionId,
            Long instructorId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.questionId = questionId;
        this.instructorId = instructorId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static QnaAnswer create(
            Long questionId,
            Long instructorId,
            String content
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new QnaAnswer(
                questionId,
                instructorId,
                content,
                now,
                now
        );
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
