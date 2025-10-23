package com.thexperience.backend.repos;

import com.thexperience.backend.entities.CommentEntity;
import com.thexperience.backend.entities.CommentLikeEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, UUID> {
    
    // Find like by comment and user
    Optional<CommentLikeEntity> findByCommentAndUser(CommentEntity comment, UserEntity user);
    
    // Find all likes for a comment
    List<CommentLikeEntity> findByCommentOrderByCreatedAtDesc(CommentEntity comment);
    
    // Find all likes by user
    List<CommentLikeEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    
    // Count likes for a comment
    long countByComment(CommentEntity comment);
    
    // Check if user has liked a comment
    boolean existsByCommentAndUser(CommentEntity comment, UserEntity user);
    
    // Delete like by comment and user
    void deleteByCommentAndUser(CommentEntity comment, UserEntity user);
}
