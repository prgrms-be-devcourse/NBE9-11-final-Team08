package com.team08.backend.domain.course.entity;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.global.common.BaseTimeEntity;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE courses SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long instructorId;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String thumbnail;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    private Course(Long instructorId, Long categoryId, String title, String description,
                   String thumbnail, int price, CourseStatus status) {
        validateInitialState(instructorId, categoryId, title, price);
        this.instructorId = instructorId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
        this.price = price;
        this.status = status;
    }

    public static Course createDraft(Long instructorId, Long categoryId, String title, String description,
                                     String thumbnail, int price) {
        return new Course(instructorId, categoryId, title, description, thumbnail, price, CourseStatus.DRAFT);
    }

    public void addChapter(Chapter chapter) {
        if (chapter == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        chapters.add(chapter);
        chapter.assignCourse(this);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void validateOwner(Long requestUserId) {
        if (!this.instructorId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
        }
    }

    public void updateThumbnail(String thumbnail) {
        if (thumbnail == null || thumbnail.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.thumbnail = thumbnail;
    }

    public void updateGeneralInfo(CourseUpdateRequest request) {
        validateInitialState(this.instructorId, request.categoryId(), request.title(), request.price());
        this.categoryId = request.categoryId();
        this.title = request.title();
        this.description = request.description();
        this.thumbnail = request.thumbnail();
        this.price = request.price();

        updateChapters(request.chapters());
    }

    private void updateChapters(List<CourseUpdateRequest.ChapterUpdateRequest> chapterRequests) {
        Set<Long> requestIds = chapterRequests.stream()
                .map(CourseUpdateRequest.ChapterUpdateRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        this.chapters.removeIf(chapter -> !requestIds.contains(chapter.getId()));

        Map<Long, Chapter> existingChapterMap = this.chapters.stream()
                .collect(Collectors.toMap(Chapter::getId, chapter -> chapter));

        for (CourseUpdateRequest.ChapterUpdateRequest chapterReq : chapterRequests) {
            if (chapterReq.id() != null && existingChapterMap.containsKey(chapterReq.id())) {
                Chapter existingChapter = existingChapterMap.get(chapterReq.id());
                existingChapter.updateGeneralInfo(chapterReq.title(), chapterReq.orderNo(), chapterReq.lectures());
            } else {
                Chapter newChapter = Chapter.create(chapterReq.title(), chapterReq.orderNo(), this);
                this.addChapter(newChapter);
                newChapter.updateGeneralInfo(chapterReq.title(), chapterReq.orderNo(), chapterReq.lectures());
            }
        }
    }

    public CourseStatusHistory requestReview(Long requestUserId) {
        if (this.status != CourseStatus.DRAFT) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        if (this.chapters.isEmpty() || this.chapters.stream().anyMatch(c -> c.getLectures().isEmpty())) {
            throw new CustomException(ErrorCode.COURSE_CURRICULUM_EMPTY);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.IN_REVIEW, requestUserId);
        this.status = CourseStatus.IN_REVIEW;

        return history;
    }

    public CourseStatusHistory cancelReview(Long requestUserId) {
        if (this.status != CourseStatus.IN_REVIEW) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.DRAFT, requestUserId);
        this.status = CourseStatus.DRAFT;

        return history;
    }

    public CourseStatusHistory approve(Long adminId) {
        if (this.status != CourseStatus.IN_REVIEW) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.ON_SALE, adminId);
        this.status = CourseStatus.ON_SALE;

        return history;
    }

    public CourseStatusHistory reject(Long adminId, String reason) {
        if (this.status != CourseStatus.IN_REVIEW) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.DRAFT, adminId, reason);
        this.status = CourseStatus.DRAFT;

        return history;
    }

    public CourseStatusHistory close(Long requestUserId) {
        validateOwner(requestUserId);

        if (this.status != CourseStatus.ON_SALE) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.SUSPENDED, requestUserId);
        this.status = CourseStatus.SUSPENDED;

        return history;
    }

    public CourseStatusHistory suspendByAdmin(Long adminId, String reason) {
        if (this.status != CourseStatus.ON_SALE) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.SUSPENDED, adminId, reason);
        this.status = CourseStatus.SUSPENDED;

        return history;
    }

    public CourseStatusHistory delete(Long requestUserId) {
        if (this.status != CourseStatus.DRAFT && this.status != CourseStatus.ON_SALE && this.status != CourseStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }

        CourseStatusHistory history = CourseStatusHistory.of(this.id, this.status, CourseStatus.DELETED, requestUserId);
        this.status = CourseStatus.DELETED;
        this.deletedAt = LocalDateTime.now();

        return history;
    }

    private void validateInitialState(Long instructorId, Long categoryId, String title, int price) {
        if (instructorId == null || categoryId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (title == null || title.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (price < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
