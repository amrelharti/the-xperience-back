package com.thexperience.backend.repos;

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
    
    // Find attendance by session and user
    Optional<LiveSessionAttendanceEntity> findBySessionAndUser(LiveSessionEntity session, UserEntity user);
    
    // Find all attendance for a session
    List<LiveSessionAttendanceEntity> findBySessionOrderByAttendedAtAsc(LiveSessionEntity session);
    
    // Find all attendance by user
    List<LiveSessionAttendanceEntity> findByUserOrderByAttendedAtDesc(UserEntity user);
    
    // Count attendance for a session
    long countBySession(LiveSessionEntity session);
    
    // Count attendance by user
    long countByUser(UserEntity user);
    
    // Check if user attended a session
    boolean existsBySessionAndUser(LiveSessionEntity session, UserEntity user);
    
    // Find attendance by user and training module (through session)
    @Query("SELECT la FROM LiveSessionAttendanceEntity la WHERE la.user = :user AND la.session.trainingModule = :trainingModule ORDER BY la.attendedAt DESC")
    List<LiveSessionAttendanceEntity> findByUserAndTrainingModule(@Param("user") UserEntity user, @Param("trainingModule") com.thexperience.backend.entities.TrainingModuleEntity trainingModule);
}
