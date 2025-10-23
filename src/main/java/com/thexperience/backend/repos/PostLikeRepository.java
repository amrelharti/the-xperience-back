package com.thexperience.backend.repos;

import com.thexperience.backend.entities.PostEntity;
import com.thexperience.backend.entities.PostLikeEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLikeEntity, UUID> {
    
    // Find like by post and user
    Optional<PostLikeEntity> findByPostAndUser(PostEntity post, UserEntity user);
    
    // Find all likes for a post
    List<PostLikeEntity> findByPostOrderByCreatedAtDesc(PostEntity post);
    
    // Find all likes for a post (simple)
    List<PostLikeEntity> findByPost(PostEntity post);
    
    // Find all likes by user
    List<PostLikeEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    
    // Count likes for a post
    long countByPost(PostEntity post);
    
    // Check if user has liked a post
    boolean existsByPostAndUser(PostEntity post, UserEntity user);
    
    // Delete like by post and user
    void deleteByPostAndUser(PostEntity post, UserEntity user);
}
