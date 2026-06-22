package com.team08.backend.domain.lecturereflection.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecture_reflections", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lecture_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureReflection extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long lectureId;
    @Column(nullable = false)
    private Long userId;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LectureReflection(Long userId, Long lectureId, String content) {
        this.userId = userId;
        this.lectureId = lectureId;
        this.content = content;
    }

    public static LectureReflection create(Long userId, Long lectureId, String content) {
        return new LectureReflection(userId, lectureId, content);
    }

    public void update(String content) {
        this.content = content;
        // updatedAt은 @LastModifiedDate가 자동 갱신
    }
}
