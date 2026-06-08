package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "lecture_reflections",
        uniqueConstraints = @UniqueConstraint(name = "uk_lecture_reflections_user_lecture", columnNames = {"user_id", "lecture_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static LectureReflection create(Lecture lecture, User user, String content) {
        LocalDateTime now = LocalDateTime.now();
        LectureReflection reflection = new LectureReflection();
        reflection.lecture = lecture;
        reflection.user = user;
        reflection.content = content;
        reflection.createdAt = now;
        reflection.updatedAt = now;
        return reflection;
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
