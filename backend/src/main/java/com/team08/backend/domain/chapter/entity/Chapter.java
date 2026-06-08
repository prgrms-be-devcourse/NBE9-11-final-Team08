package com.team08.backend.domain.chapter.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.entity.Lecture;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "chapters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer orderNo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Chapter(Course course, String title, Integer orderNo) {
        this.course = course;
        this.title = title;
        this.orderNo = orderNo;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, Integer orderNo) {
        if (title != null && !title.isBlank()) this.title = title;
        if (orderNo != null && orderNo >= 0) this.orderNo = orderNo;
        this.updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    public void setCourse(Course course) {
        this.course = course;
    }
}