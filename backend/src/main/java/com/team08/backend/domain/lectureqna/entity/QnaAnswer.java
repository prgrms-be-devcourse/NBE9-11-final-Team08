package com.team08.backend.domain.lectureqna.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "qna_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long questionId;
    @Column(nullable = false)
    private Long instructorId;
    @Lob @Column(nullable = false)
    private String content;

    private QnaAnswer(Long questionId, Long instructorId, String content) {
        this.questionId = questionId;
        this.instructorId = instructorId;
        this.content = content;
    }

    public static QnaAnswer create(Long questionId, Long instructorId, String content) {
        return new QnaAnswer(questionId, instructorId, content);
    }

    public void update(String content) {
        this.content = content;
        // updatedAt은 @LastModifiedDate가 자동 갱신
    }
}
