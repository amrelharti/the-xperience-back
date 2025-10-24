package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.LiveSessionAttendanceEntity;
import com.thexperience.backend.entities.LiveSessionEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveSessionAttendanceRepository extends JpaRepository<LiveSessionAttendanceEntity, UUID> {
    
    // Find attendance by session, user, and class
    Optional<LiveSessionAttendanceEntity> findBySessionAndUserAndClassEntity(LiveSessionEntity session, UserEntity user, ClassEntity classEntity);
    
    // Find all attendance for a session in a specific class
    List<LiveSessionAttendanceEntity> findBySessionAndClassEntityOrderByAttendedAtAsc(LiveSessionEntity session, ClassEntity classEntity);
    
    // Find all attendance by user in a specific class
    List<LiveSessionAttendanceEntity> findByUserAndClassEntityOrderByAttendedAtDesc(UserEntity user, ClassEntity classEntity);
    
    // Count attendance for a session in a specific class
    long countBySessionAndClassEntity(LiveSessionEntity session, ClassEntity classEntity);
    
    // Count attendance by user in a specific class
    long countByUserAndClassEntity(UserEntity user, ClassEntity classEntity);
    
    // Check if user attended a session in a specific class
    boolean existsBySessionAndUserAndClassEntity(LiveSessionEntity session, UserEntity user, ClassEntity classEntity);
    
    // Find attendance by user, training module, and class (through session)
    @Query("SELECT la FROM LiveSessionAttendanceEntity la WHERE la.user = :user AND la.session.trainingModule = :trainingModule AND la.classEntity = :classEntity ORDER BY la.attendedAt DESC")
    List<LiveSessionAttendanceEntity> findByUserAndTrainingModuleAndClassEntity(@Param("user") UserEntity user, @Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule, @Param("classEntity") ClassEntity classEntity);
}
