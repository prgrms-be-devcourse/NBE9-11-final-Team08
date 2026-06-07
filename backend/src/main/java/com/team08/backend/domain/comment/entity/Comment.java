package com.team08.backend.domain.comment.entity;

import com.team08.backend.domain.post.entity.Post;
import com.team08.backend.domain.study.exception.StudyAccessDeniedException;
import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Comment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public static Comment create(Post post, User user, String content) {
        return new Comment(post, user, content);
    }

    public void update(Long userId, String content) {
        validateWriter(userId);

        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete(Long userId) {
        if (!isWriter(userId) && !post.getStudy().isOwner(userId)) {
            throw new StudyAccessDeniedException();
        }

        deletedAt = LocalDateTime.now();
    }

    public String getDisplayContent() {
        return isDeleted() ? "삭제된 댓글입니다." : content;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void validateWriter(Long userId) {
        if (!isWriter(userId)) {
            throw new StudyAccessDeniedException();
        }
    }

    private boolean isWriter(Long userId) {
        return user.getId().equals(userId);
    }
}
