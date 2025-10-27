package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ModuleAccessCodeEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleAccessCodeRepository extends JpaRepository<ModuleAccessCodeEntity, UUID> {
    List<ModuleAccessCodeEntity> findByModule(ModuleEntity module);
    Optional<ModuleAccessCodeEntity> findFirstByModuleAndCodeHashAndConsumedAtIsNull(ModuleEntity module, String codeHash);
    
    // CRITICAL FIX: Find code that belongs to a specific training module
    Optional<ModuleAccessCodeEntity> findFirstByTrainingModuleAndCodeHashAndConsumedAtIsNull(TrainingModuleEntity trainingModule, String codeHash);
    List<ModuleAccessCodeEntity> findByTrainingModule(TrainingModuleEntity trainingModule);
    
    // Check if user has consumed a code for this training module in this specific class
    boolean existsByTrainingModuleAndConsumedByAndConsumedByClassIdAndConsumedAtIsNotNull(
        TrainingModuleEntity trainingModule, 
        com.thexperience.backend.entities.UserEntity user, 
        UUID classId
    );
}
