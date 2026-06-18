package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE lectures SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
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

    @Builder
    public Lecture(String m3u8Path, String title, String summary, int durationSeconds, int orderNo, boolean isFreePreview, Chapter chapter) {
        this.m3u8Path = m3u8Path;
        this.title = title;
        this.summary = summary;
        this.durationSeconds = durationSeconds;
        this.orderNo = orderNo;
        this.isFreePreview = isFreePreview;
        this.chapter = chapter;
    }

    public static Lecture create(String title, int durationSeconds, int orderNo, boolean isFreePreview, Chapter chapter) {
        return Lecture.builder()
                .title(title)
                .m3u8Path("")
                .durationSeconds(durationSeconds)
                .orderNo(orderNo)
                .isFreePreview(isFreePreview)
                .chapter(chapter)
                .build();
    }

    public void assignChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void updateGeneralInfo(String title, String summary, int durationSeconds, int orderNo, boolean isFreePreview) {
        this.title = title;
        this.summary = summary;
        this.durationSeconds = durationSeconds;
        this.orderNo = orderNo;
        this.isFreePreview = isFreePreview;
    }

    public void updateM3u8Path(String m3u8Path) {
        this.m3u8Path = m3u8Path;
    }

    public void updateOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }
}