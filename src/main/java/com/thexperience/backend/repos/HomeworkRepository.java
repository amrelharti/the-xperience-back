package com.thexperience.backend.repos;

import com.thexperience.backend.entities.HomeworkEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<HomeworkEntity, UUID> {
    
    // Find homeworks by module
    List<HomeworkEntity> findByModuleOrderByCreatedAtDesc(ModuleEntity module);
    
    // Find homeworks by author
    List<HomeworkEntity> findByAuthorOrderByCreatedAtDesc(UserEntity author);
    
    // Find homeworks by module with pagination
    Page<HomeworkEntity> findByModuleAndIsHiddenFalseOrderByCreatedAtDesc(ModuleEntity module, Pageable pageable);
    
    // Find all visible homeworks for global feed
    Page<HomeworkEntity> findByIsHiddenFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Find homeworks by author with pagination
    Page<HomeworkEntity> findByAuthorOrderByCreatedAtDesc(UserEntity author, Pageable pageable);
    
    // Count homeworks by author
    long countByAuthor(UserEntity author);
    
    // Find homeworks with average score above threshold
    @Query("SELECT h FROM HomeworkEntity h WHERE h.avgScore >= :minScore AND h.isHidden = false ORDER BY h.avgScore DESC")
    List<HomeworkEntity> findTopRatedHomeworks(@Param("minScore") Double minScore);
    
    // Search homeworks by title or body
    @Query("SELECT h FROM HomeworkEntity h WHERE (LOWER(h.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.body) LIKE LOWER(CONCAT('%', :query, '%'))) AND h.isHidden = false ORDER BY h.createdAt DESC")
    Page<HomeworkEntity> searchHomeworks(@Param("query") String query, Pageable pageable);
    
    // Find homeworks by author (for leaderboard calculations)
    List<HomeworkEntity> findByAuthor(UserEntity author);
    
    // Find homeworks by module and author (for module leaderboard calculations)
    List<HomeworkEntity> findByModuleAndAuthor(ModuleEntity module, UserEntity author);
}
