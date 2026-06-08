package com.team08.backend.domain.course.entity;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.user.entity.User;
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
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    private String thumbnail;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    public Course(User instructor, Category category, String title, String description, String thumbnail, Integer price) {
        this.instructor = instructor;
        this.category = category;
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
        this.price = price;
        this.status = CourseStatus.DRAFT;
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String description, String thumbnail, Integer price, Category category) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
        if (thumbnail != null) this.thumbnail = thumbnail;
        if (price != null && price >= 0) this.price = price;
        if (category != null) this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.status = CourseStatus.SUSPENDED;
    }

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    public void clearChapters() {
        this.chapters.clear();
    }
}
