package com.team08.backend.domain.lecture.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lectures")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long chapterId;
    @Column(nullable = false)
    private String m3u8Path;
    @Column(nullable = false)
    private String title;
    private String summary;
    @Column(nullable = false)
    private Integer durationSeconds;
    @Column(nullable = false)
    private Integer orderNo;
    @Column(nullable = false)
    private Boolean isFreePreview = false;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
