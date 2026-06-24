package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.global.common.BaseTimeEntity;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lectures", indexes = {
        @Index(name = "idx_lecture_chapter_id", columnList = "chapter_id")
})
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

    @Column(name = "video_uuid", length = 36)
    private String videoUuid;

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

    private Lecture(String m3u8Path, String videoUuid, String title, String summary, int durationSeconds, int orderNo, boolean isFreePreview, Chapter chapter) {
        validateInitialState(title, durationSeconds, orderNo, chapter);
        validateVideoUuid(videoUuid);
        this.m3u8Path = m3u8Path;
        this.videoUuid = videoUuid;
        this.title = title;
        this.summary = summary;
        this.durationSeconds = durationSeconds;
        this.orderNo = orderNo;
        this.isFreePreview = isFreePreview;
        this.chapter = chapter;
    }

    public static Lecture createDraft(String title, String summary, int durationSeconds, int orderNo, boolean isFreePreview, Chapter chapter) {
        return new Lecture("", "", title, summary, durationSeconds, orderNo, isFreePreview, chapter);
    }

    public static Lecture createWithStream(String m3u8Path, String videoUuid, String title, String summary, int durationSeconds, int orderNo, boolean isFreePreview, Chapter chapter) {
        if (m3u8Path == null || m3u8Path.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new Lecture(m3u8Path, videoUuid, title, summary, durationSeconds, orderNo, isFreePreview, chapter);
    }

    public void assignChapter(Chapter chapter) {
        if (chapter == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.chapter = chapter;
    }

    public void updateGeneralInfo(String title, String summary, int durationSeconds, int orderNo, boolean isFreePreview) {
        validateGeneralFields(title, durationSeconds, orderNo);
        this.title = title;
        this.summary = summary;
        this.durationSeconds = durationSeconds;
        this.orderNo = orderNo;
        this.isFreePreview = isFreePreview;
    }

    public void updateM3u8Path(String m3u8Path, String videoUuid) {
        if (m3u8Path == null || m3u8Path.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        validateVideoUuid(videoUuid);
        this.m3u8Path = m3u8Path;
        this.videoUuid = videoUuid;
    }

    public void updateOrderNo(int orderNo) {
        if (orderNo < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.orderNo = orderNo;
    }

    private void validateInitialState(String title, int durationSeconds, int orderNo, Chapter chapter) {
        validateGeneralFields(title, durationSeconds, orderNo);
        if (chapter == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateGeneralFields(String title, int durationSeconds, int orderNo) {
        if (title == null || title.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (durationSeconds <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (orderNo < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateVideoUuid(String videoUuid) {
        if (videoUuid == null || videoUuid.isBlank()) {
            return;
        }
        try {
            UUID.fromString(videoUuid);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}