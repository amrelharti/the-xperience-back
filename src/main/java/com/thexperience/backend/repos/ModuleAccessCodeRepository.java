package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.ModuleAccessCodeEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for multi-use, class-specific training module access codes.
 * Each code is tied to a TrainingModule + Class combination.
 */
public interface ModuleAccessCodeRepository extends JpaRepository<ModuleAccessCodeEntity, UUID> {
    
    // Find all codes for a specific training module and class
    List<ModuleAccessCodeEntity> findByTrainingModuleAndClassEntity(
        TrainingModuleEntity trainingModule, 
        ClassEntity classEntity
    );
    
    // Find active code for a specific training module and class
    Optional<ModuleAccessCodeEntity> findFirstByTrainingModuleAndClassEntityAndIsActiveTrue(
        TrainingModuleEntity trainingModule, 
        ClassEntity classEntity
    );
    
    // Verify code by hash for a specific training module and class
    Optional<ModuleAccessCodeEntity> findFirstByTrainingModuleAndClassEntityAndCodeHashAndIsActiveTrue(
        TrainingModuleEntity trainingModule, 
        ClassEntity classEntity,
        String codeHash
    );
    
    // Find all codes for a training module (across all classes) - for admin overview
    List<ModuleAccessCodeEntity> findByTrainingModule(TrainingModuleEntity trainingModule);
    
    // Find all active codes for a training module (across all classes)
    List<ModuleAccessCodeEntity> findByTrainingModuleAndIsActiveTrue(TrainingModuleEntity trainingModule);
}
