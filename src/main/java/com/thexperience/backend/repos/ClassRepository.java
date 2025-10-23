package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, UUID> {
    
    // Find classes by module
    List<ClassEntity> findByModuleOrderByStartDateDesc(ModuleEntity module);
    
    // Find active classes by module
    List<ClassEntity> findByModuleAndIsActiveTrueAndIsArchivedFalseOrderByStartDateDesc(ModuleEntity module);
    
    // Find classes created by admin
    List<ClassEntity> findByCreatedByOrderByCreatedAtDesc(UserEntity createdBy);
    
    // Find active classes
    List<ClassEntity> findByIsActiveTrueAndIsArchivedFalseOrderByStartDateDesc();
    
    // Find archived classes
    List<ClassEntity> findByIsArchivedTrueOrderByStartDateDesc();
    
    // Find all non-archived classes (both active and inactive)
    List<ClassEntity> findByIsArchivedFalseOrderByStartDateDesc();
    
    // Find classes by date range
    @Query("SELECT c FROM ClassEntity c WHERE c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date) AND c.isActive = true AND c.isArchived = false")
    List<ClassEntity> findActiveClassesByDate(@Param("date") LocalDate date);
    
    // Find classes for a specific module and date
    @Query("SELECT c FROM ClassEntity c WHERE c.module = :module AND c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date) AND c.isActive = true AND c.isArchived = false")
    List<ClassEntity> findActiveClassesByModuleAndDate(@Param("module") ModuleEntity module, @Param("date") LocalDate date);
}


