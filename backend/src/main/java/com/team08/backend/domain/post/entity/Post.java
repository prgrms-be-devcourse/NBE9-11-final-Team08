package com.team08.backend.domain.post.entity;

import com.team08.backend.domain.study.exception.StudyAccessDeniedException;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User writer;

    @Column(length = 255, nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Post(Study study, User writer, String title, String content, PostType type) {
        validateNoticePermission(study, writer.getId(), type);

        this.study = study;
        this.writer = writer;
        this.title = title;
        this.content = content;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public static Post create(Study study, User user, String title, String content, PostType type) {
        return new Post(study, user, title, content, type);
    }

    public void update(Long userId, String title, String content, PostType type) {
        validateWriter(userId);
        validateNoticePermission(study, userId, type);

        this.title = title;
        this.content = content;
        this.type = type;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete(Long userId) {
        if (!isWriter(userId) && !study.isOwner(userId)) {
            throw new StudyAccessDeniedException();
        }

        deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isWriter(Long userId) {
        return writer.getId().equals(userId);
    }

    private void validateWriter(Long userId) {
        if (!isWriter(userId)) {
            throw new StudyAccessDeniedException();
        }
    }

    private static void validateNoticePermission(Study study, Long userId, PostType type) {
        if (type == PostType.NOTICE && !study.isOwner(userId)) {
            throw new StudyAccessDeniedException();
        }
    }
}
