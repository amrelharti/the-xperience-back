package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.CourseProgressEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgressEntity, UUID> {
    
    // Find progress by user, course, and class
    Optional<CourseProgressEntity> findByUserAndCourseAndClassEntity(UserEntity user, CourseEntity course, ClassEntity classEntity);
    
    // Check if user has completed a specific course in a specific class
    boolean existsByUserAndCourseAndClassEntityAndStatus(UserEntity user, CourseEntity course, ClassEntity classEntity, String status);
    
    // Find all progress for a user in a specific class
    List<CourseProgressEntity> findByUserAndClassEntityOrderByStartedAtDesc(UserEntity user, ClassEntity classEntity);
    
    // Find completed courses for a user in a specific class
    List<CourseProgressEntity> findByUserAndClassEntityAndStatusOrderByCompletedAtDesc(UserEntity user, ClassEntity classEntity, String status);
    
    // Find progress for courses in a specific training module and class
    @Query("SELECT cp FROM CourseProgressEntity cp WHERE cp.user = :user AND cp.course.trainingModule = :trainingModule AND cp.classEntity = :classEntity ORDER BY cp.course.orderIndex ASC")
    List<CourseProgressEntity> findByUserAndCourseTrainingModuleAndClassEntity(@Param("user") UserEntity user, @Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule, @Param("classEntity") ClassEntity classEntity);
    
    // Count completed courses for a user in a specific training module and class
    @Query("SELECT COUNT(cp) FROM CourseProgressEntity cp WHERE cp.user = :user AND cp.course.trainingModule = :trainingModule AND cp.classEntity = :classEntity AND cp.status = 'COMPLETED'")
    long countCompletedCoursesByUserAndTrainingModuleAndClassEntity(@Param("user") UserEntity user, @Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule, @Param("classEntity") ClassEntity classEntity);
    
    // Get completion statistics for a course
    @Query("SELECT COUNT(cp) FROM CourseProgressEntity cp WHERE cp.course = :course AND cp.status = 'COMPLETED'")
    long countCompletedByCourse(@Param("course") CourseEntity course);
    
    // Get completion statistics for a training module
    @Query("SELECT COUNT(cp) FROM CourseProgressEntity cp WHERE cp.course.trainingModule = :trainingModule AND cp.status = 'COMPLETED'")
    long countCompletedByTrainingModule(@Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule);
    
    // Analytics queries
    long countByStatus(String status);
    
    @Query("SELECT AVG(cp.watchTimeSeconds) FROM CourseProgressEntity cp WHERE cp.watchTimeSeconds IS NOT NULL")
    Double findAverageWatchTimeSeconds();
    
    List<CourseProgressEntity> findByCourse(CourseEntity course);
    
    List<CourseProgressEntity> findByUser(UserEntity user);
    
    List<CourseProgressEntity> findByUserAndClassEntity(UserEntity user, ClassEntity classEntity);
    
    @Query("SELECT cp FROM CourseProgressEntity cp WHERE cp.completedAt >= :date")
    List<CourseProgressEntity> findByCompletedAtAfter(@Param("date") java.time.Instant date);
}


