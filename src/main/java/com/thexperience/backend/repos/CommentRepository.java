package com.thexperience.backend.repos;

import com.thexperience.backend.entities.CommentEntity;
import com.thexperience.backend.entities.PostEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {
    
    // Find comments by post
    List<CommentEntity> findByPostOrderByCreatedAtAsc(PostEntity post);
    
    // Find comments by post with pagination
    Page<CommentEntity> findByPostOrderByCreatedAtAsc(PostEntity post, Pageable pageable);
    
    // Find comments by author
    List<CommentEntity> findByAuthorOrderByCreatedAtDesc(UserEntity author);
    
    // Find comments by author with pagination
    Page<CommentEntity> findByAuthorOrderByCreatedAtDesc(UserEntity author, Pageable pageable);
    
    // Find top-level comments (no parent)
    List<CommentEntity> findByPostAndParentCommentIsNullOrderByCreatedAtAsc(PostEntity post);
    
    // Find replies to a comment
    List<CommentEntity> findByParentCommentOrderByCreatedAtAsc(CommentEntity parentComment);
    
    // Find comments by post and visibility
    List<CommentEntity> findByPostAndIsHiddenFalseOrderByCreatedAtAsc(PostEntity post);
    
    // Count comments by post
    long countByPost(PostEntity post);
    
    // Count comments by author
    long countByAuthor(UserEntity author);
    
    // Find comments by post (for leaderboard calculations)
    List<CommentEntity> findByPost(PostEntity post);
    
    // Find comments by author (for leaderboard calculations)
    List<CommentEntity> findByAuthor(UserEntity author);
}
