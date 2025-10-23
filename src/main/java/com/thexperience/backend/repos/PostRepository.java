package com.thexperience.backend.repos;

import com.thexperience.backend.entities.PostEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.entities.ClassEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {
    
    // Find posts by author
    List<PostEntity> findByAuthorOrderByCreatedAtDesc(UserEntity author);
    
    // Find posts by module
    List<PostEntity> findByModuleOrderByCreatedAtDesc(ModuleEntity module);
    
    // Find posts by postType
    List<PostEntity> findByPostTypeOrderByCreatedAtDesc(PostEntity.PostType postType);
    
    // Find posts by author with pagination
    Page<PostEntity> findByAuthorOrderByCreatedAtDesc(UserEntity author, Pageable pageable);
    
    // Find posts by module with pagination
    Page<PostEntity> findByModuleOrderByCreatedAtDesc(ModuleEntity module, Pageable pageable);
    
    // Find posts by postType with pagination
    Page<PostEntity> findByPostTypeOrderByCreatedAtDesc(PostEntity.PostType postType, Pageable pageable);
    
    // Find all visible posts for feed
    Page<PostEntity> findByIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);
    
    // Find posts by postType and visibility
    Page<PostEntity> findByPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(PostEntity.PostType postType, Pageable pageable);
    
    // Find posts by module and visibility
    Page<PostEntity> findByModuleAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(ModuleEntity module, Pageable pageable);
    
    // Find posts by class and visibility
    Page<PostEntity> findByClassEntityAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(ClassEntity classEntity, Pageable pageable);
    
    // Find assignment posts
    Page<PostEntity> findByIsAssignmentTrueAndIsHiddenFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Search posts by content
    @Query("SELECT p FROM PostEntity p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) AND p.isHidden = false ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<PostEntity> searchPosts(@Param("query") String query, Pageable pageable);
    
    // Count posts by author
    long countByAuthor(UserEntity author);
    
    // Count posts by postType
    long countByPostType(PostEntity.PostType postType);
    
    // Find posts by author (for leaderboard calculations)
    List<PostEntity> findByAuthor(UserEntity author);
    
    // Find posts by module and author (for module leaderboard calculations)
    List<PostEntity> findByModuleAndAuthor(ModuleEntity module, UserEntity author);
    
    // Find assignment posts by author
    List<PostEntity> findByAuthorAndIsAssignmentTrue(UserEntity author);
    
    // Find assignment posts by author and module
    List<PostEntity> findByAuthorAndModuleAndIsAssignmentTrue(UserEntity author, ModuleEntity module);
    
    // Find assignment posts by author and class (for class leaderboard)
    List<PostEntity> findByAuthorAndClassEntityAndIsAssignmentTrue(UserEntity author, ClassEntity classEntity);
    
    // Find posts by multiple class IDs (for class-based feed filtering)
    Page<PostEntity> findByClassEntityIdInAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(List<UUID> classIds, Pageable pageable);
    
    // Find posts by multiple class IDs and post type
    Page<PostEntity> findByClassEntityIdInAndPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(List<UUID> classIds, PostEntity.PostType postType, Pageable pageable);
    
    // Admin feed methods - find posts by class ID
    Page<PostEntity> findByClassEntityIdAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(UUID classId, Pageable pageable);
    
    // Find posts by class ID and post type
    Page<PostEntity> findByClassEntityIdAndPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(UUID classId, PostEntity.PostType postType, Pageable pageable);
}
