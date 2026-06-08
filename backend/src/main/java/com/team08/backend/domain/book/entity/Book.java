package com.team08.backend.domain.book.entity;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String publisher;

    @Lob
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Boolean isEbook;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    private Integer viewCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder
    public Book(User seller, Category category, String title, String author, String publisher,
                String description, Integer price, Boolean isEbook) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.description = description;
        this.price = price;
        this.isEbook = isEbook;
        this.status = CourseStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String author, String publisher, String description,
                       Integer price, Boolean isEbook, Category category) {
        if (title != null && !title.isBlank()) this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.description = description;
        this.price = price;
        this.isEbook = isEbook;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.status = CourseStatus.SUSPENDED;
    }
}