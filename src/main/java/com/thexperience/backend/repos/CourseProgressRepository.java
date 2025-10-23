package com.thexperience.backend.repos;

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
    
    // Find progress by user and course
    Optional<CourseProgressEntity> findByUserAndCourse(UserEntity user, CourseEntity course);
    
    // Check if user has completed a specific course
    boolean existsByUserAndCourseAndStatus(UserEntity user, CourseEntity course, String status);
    
    // Find all progress for a user
    List<CourseProgressEntity> findByUserOrderByStartedAtDesc(UserEntity user);
    
    // Find completed courses for a user
    List<CourseProgressEntity> findByUserAndStatusOrderByCompletedAtDesc(UserEntity user, String status);
    
    // Find progress for courses in a specific training module
    @Query("SELECT cp FROM CourseProgressEntity cp WHERE cp.user = :user AND cp.course.trainingModule = :trainingModule ORDER BY cp.course.orderIndex ASC")
    List<CourseProgressEntity> findByUserAndCourseTrainingModule(@Param("user") UserEntity user, @Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule);
    
    // Count completed courses for a user in a specific training module
    @Query("SELECT COUNT(cp) FROM CourseProgressEntity cp WHERE cp.user = :user AND cp.course.trainingModule = :trainingModule AND cp.status = 'COMPLETED'")
    long countCompletedCoursesByUserAndTrainingModule(@Param("user") UserEntity user, @Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule);
    
    // Get completion statistics for a course
    @Query("SELECT COUNT(cp) FROM CourseProgressEntity cp WHERE cp.course = :course AND cp.status = 'COMPLETED'")
    long countCompletedByCourse(@Param("course") CourseEntity course);
    
    // Get completion statistics for a training module
    @Query("SELECT COUNT(cp) FROM CourseProgressEntity cp WHERE cp.course.trainingModule = :trainingModule AND cp.status = 'COMPLETED'")
    long countCompletedByTrainingModule(@Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule);
}


