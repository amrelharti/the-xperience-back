package com.thexperience.backend.repos;

import com.thexperience.backend.entities.HomeworkEntity;
import com.thexperience.backend.entities.HomeworkScoreEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkScoreRepository extends JpaRepository<HomeworkScoreEntity, UUID> {
    
    // Find score by homework and user
    Optional<HomeworkScoreEntity> findByHomeworkAndUser(HomeworkEntity homework, UserEntity user);
    
    // Find all scores for a homework
    List<HomeworkScoreEntity> findByHomeworkOrderByCreatedAtDesc(HomeworkEntity homework);
    
    // Find all scores by user
    List<HomeworkScoreEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    
    // Count scores for a homework
    long countByHomework(HomeworkEntity homework);
    
    // Calculate average score for a homework
    @Query("SELECT AVG(hs.value) FROM HomeworkScoreEntity hs WHERE hs.homework = :homework")
    Double calculateAverageScore(@Param("homework") HomeworkEntity homework);
    
    // Check if user has scored a homework
    boolean existsByHomeworkAndUser(HomeworkEntity homework, UserEntity user);
    
    // Find scores by value range
    @Query("SELECT hs FROM HomeworkScoreEntity hs WHERE hs.value BETWEEN :minValue AND :maxValue ORDER BY hs.createdAt DESC")
    List<HomeworkScoreEntity> findByValueRange(@Param("minValue") Integer minValue, @Param("maxValue") Integer maxValue);
}
