package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.LiveSessionEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveSessionRepository extends JpaRepository<LiveSessionEntity, UUID> {
    
    // Find sessions by training module
    List<LiveSessionEntity> findByTrainingModuleOrderByStartsAtAsc(TrainingModuleEntity trainingModule);
    
    // Find single session by training module (for admin management) - DEPRECATED: Use findByTrainingModuleAndClassEntity instead
    @Deprecated
    Optional<LiveSessionEntity> findByTrainingModule(TrainingModuleEntity trainingModule);
    
    // Find active session by training module (backward compatibility)
    Optional<LiveSessionEntity> findByTrainingModuleAndIsArchivedFalse(TrainingModuleEntity trainingModule);
    
    // Find single session by training module AND class (for per-class live sessions)
    Optional<LiveSessionEntity> findByTrainingModuleAndClassEntity(TrainingModuleEntity trainingModule, ClassEntity classEntity);
    
    // Find active (non-archived) session by training module AND class
    Optional<LiveSessionEntity> findByTrainingModuleAndClassEntityAndIsArchivedFalse(TrainingModuleEntity trainingModule, ClassEntity classEntity);
    
    // Find all sessions by training module and class
    List<LiveSessionEntity> findByTrainingModuleAndClassEntityOrderByStartsAtAsc(TrainingModuleEntity trainingModule, ClassEntity classEntity);
    
    // Find upcoming sessions
    @Query("SELECT ls FROM LiveSessionEntity ls WHERE ls.startsAt > :now ORDER BY ls.startsAt ASC")
    List<LiveSessionEntity> findUpcomingSessions(@Param("now") Instant now);
    
    // Find past sessions
    @Query("SELECT ls FROM LiveSessionEntity ls WHERE ls.endsAt < :now ORDER BY ls.endsAt DESC")
    List<LiveSessionEntity> findPastSessions(@Param("now") Instant now);
    
    // Find sessions in date range
    @Query("SELECT ls FROM LiveSessionEntity ls WHERE ls.startsAt BETWEEN :startDate AND :endDate ORDER BY ls.startsAt ASC")
    List<LiveSessionEntity> findSessionsInDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Find sessions by training module and date range
    @Query("SELECT ls FROM LiveSessionEntity ls WHERE ls.trainingModule = :trainingModule AND ls.startsAt BETWEEN :startDate AND :endDate ORDER BY ls.startsAt ASC")
    List<LiveSessionEntity> findSessionsByTrainingModuleAndDateRange(@Param("trainingModule") TrainingModuleEntity trainingModule, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Find sessions by training module, class, and date range
    @Query("SELECT ls FROM LiveSessionEntity ls WHERE ls.trainingModule = :trainingModule AND ls.classEntity = :classEntity AND ls.startsAt BETWEEN :startDate AND :endDate AND ls.isArchived = false ORDER BY ls.startsAt ASC")
    List<LiveSessionEntity> findSessionsByTrainingModuleAndClassAndDateRange(@Param("trainingModule") TrainingModuleEntity trainingModule, @Param("classEntity") ClassEntity classEntity, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Count sessions by training module
    long countByTrainingModule(TrainingModuleEntity trainingModule);
    
    // Count active sessions by training module and class
    long countByTrainingModuleAndClassEntityAndIsArchivedFalse(TrainingModuleEntity trainingModule, ClassEntity classEntity);
}
