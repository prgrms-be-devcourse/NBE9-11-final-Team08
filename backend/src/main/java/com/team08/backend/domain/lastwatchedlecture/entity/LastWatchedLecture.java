package com.team08.backend.domain.lastwatchedlecture.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "last_watched_lectures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_last_watched_user_course",
                columnNames = {"user_id", "course_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LastWatchedLecture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    private LastWatchedLecture(Long userId, Long courseId, Long lectureId) {
        this.userId = userId;
        this.courseId = courseId;
        this.lectureId = lectureId;
    }

    public static LastWatchedLecture of(Long userId, Long courseId, Long lectureId) {
        return new LastWatchedLecture(userId, courseId, lectureId);
    }

    public void changeLecture(Long lectureId) {
        this.lectureId = lectureId;
    }
}
