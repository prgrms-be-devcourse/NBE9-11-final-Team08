package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.chapter.entity.Chapter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "lectures")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private String videoId;

    @Column(nullable = false)
    private String title;

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

    @Builder
    public Lecture(Chapter chapter, String videoId, String title, Integer durationSeconds, Integer orderNo, Boolean isFreePreview) {
        this.chapter = chapter;
        this.videoId = videoId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.orderNo = orderNo;
        this.isFreePreview = isFreePreview != null ? isFreePreview : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String videoId, String title, Integer durationSeconds, Integer orderNo, Boolean isFreePreview) {
        if (videoId != null && !videoId.isBlank()) this.videoId = videoId;
        if (title != null && !title.isBlank()) this.title = title;
        if (durationSeconds != null && durationSeconds >= 0) this.durationSeconds = durationSeconds;
        if (orderNo != null && orderNo >= 0) this.orderNo = orderNo;
        if (isFreePreview != null) this.isFreePreview = isFreePreview;
        this.updatedAt = LocalDateTime.now();
    }
}