package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "studies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StudyStatus status;

    private Study(User owner, Course course, String title, String description, StudyStatus status) {
        this.owner = owner;
        this.course = course;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public static Study createForCourse(User owner, Course course, String title, String description) {
        return new Study(owner, course, title, description, StudyStatus.DRAFT);
    }
}
