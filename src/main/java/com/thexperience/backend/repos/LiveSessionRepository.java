package com.thexperience.backend.repos;

import com.thexperience.backend.entities.LiveSessionEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LiveSessionRepository extends JpaRepository<LiveSessionEntity, UUID> {
    
    // Find sessions by training module
    List<LiveSessionEntity> findByTrainingModuleOrderByStartsAtAsc(TrainingModuleEntity trainingModule);
    
    // Find single session by training module (for admin management)
    java.util.Optional<LiveSessionEntity> findByTrainingModule(TrainingModuleEntity trainingModule);
    
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
    
    // Count sessions by training module
    long countByTrainingModule(TrainingModuleEntity trainingModule);
}
