package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lectures")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String m3u8Path;

    @Column(nullable = false)
    private String title;

    private String summary;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(nullable = false)
    private boolean isFreePreview = false;

    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    public void assignChapter(Chapter chapter) {
        this.chapter = chapter;
    }
}
