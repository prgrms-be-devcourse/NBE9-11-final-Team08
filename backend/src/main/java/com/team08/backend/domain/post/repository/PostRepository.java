package com.team08.backend.domain.post.repository;

import com.team08.backend.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStudyIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long studyId);

    Optional<Post> findByIdAndStudyIdAndDeletedAtIsNull(Long id, Long studyId);
}
