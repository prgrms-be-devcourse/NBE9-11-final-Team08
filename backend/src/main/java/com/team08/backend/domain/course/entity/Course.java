package com.team08.backend.domain.course.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long instructorId;
    private Long categoryId;
    @Column(nullable = false)
    private String title;
    @Lob
    private String description;
    private String thumbnail;
    @Column(nullable = false)
    private Integer price;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CourseStatus status;
    @Column(nullable = false)
    private Integer viewCount = 0;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
