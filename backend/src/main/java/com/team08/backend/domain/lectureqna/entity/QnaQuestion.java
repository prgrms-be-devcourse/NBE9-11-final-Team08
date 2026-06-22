package com.team08.backend.domain.lectureqna.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaQuestion extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long lectureId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime deletedAt;

    private QnaQuestion(Long userId, Long lectureId, String title, String content) {
        this.lectureId = lectureId;
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public static QnaQuestion create(Long userId, Long lectureId, String title, String content) {
        return new QnaQuestion(userId, lectureId, title, content);
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        // updatedAt은 @LastModifiedDate가 자동 갱신
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
