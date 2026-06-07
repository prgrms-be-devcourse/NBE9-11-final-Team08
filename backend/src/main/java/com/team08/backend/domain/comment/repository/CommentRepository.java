package com.team08.backend.domain.comment.repository;

import com.team08.backend.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    Optional<Comment> findByIdAndPostId(Long id, Long postId);
}
