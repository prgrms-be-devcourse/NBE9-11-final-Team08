package com.team08.backend.domain.lecturereflection.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_reflections", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lecture_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureReflection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long lectureId;
    @Column(nullable = false)
    private Long userId;
    @Lob @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LectureReflection(Long userId, Long lectureId, String content) {
        this.userId = userId;
        this.lectureId = lectureId;
        this.content = content;

        LocalDateTime now=LocalDateTime.now();
        this.createdAt=now;
        this.updatedAt =now;
    }

    public static LectureReflection create(Long userId, Long lectureId, String content) {
        return new LectureReflection(userId, lectureId, content);
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
